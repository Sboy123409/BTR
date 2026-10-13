package com.example.btrapp.blocking.web

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.util.Log
import com.example.btrapp.blocking.R
import com.example.btrapp.blocking.appLockGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * An on-device "VPN" that only carries DNS: the tunnel claims a fake DNS server address, so every
 * lookup comes here while all other traffic goes straight out. Adult domains get NXDOMAIN; the rest
 * are forwarded to Cloudflare's family resolver, which also filters adult and malware sites.
 */
class WebFilterVpnService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val forwarding = Dispatchers.IO.limitedParallelism(MAX_IN_FLIGHT)

    private var tunnel: ParcelFileDescriptor? = null
    private var loopJob: Job? = null
    private var cooldownJob: Job? = null
    private val settings by lazy { appLockGraph.webFilterSettings }

    /** The upstream that answered last, tried first next time so a blocked one doesn't slow every lookup. */
    @Volatile
    private var preferredUpstream: InetAddress? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!settings.settle()) {
            shutDown()
            return START_NOT_STICKY
        }
        if (loopJob?.isActive != true) start()
        watchCooldown()
        return START_STICKY
    }

    private fun start() {
        val tun = try {
            Builder()
                .setSession("BTR Web Filter")
                .addAddress(TUN_V4, 32)
                .addDnsServer(DNS_V4)
                .addRoute(DNS_V4, 32)
                .addAddress(TUN_V6, 128)
                .addDnsServer(DNS_V6)
                .addRoute(DNS_V6, 128)
                .setBlocking(true)
                .apply { openAppIntent()?.let(::setConfigureIntent) }
                .establish()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't start the tunnel", e)
            null
        } ?: return shutDown() // Not prepared (no VPN consent) or another always-on VPN owns the slot.

        tunnel = tun
        _running.value = true
        loopJob = scope.launch {
            val blocklist = resources.openRawResource(R.raw.adult_domains)
                .reader()
                .use(DomainBlocklist::parse)
            runLoop(tun, blocklist)
        }
    }

    private suspend fun runLoop(tun: ParcelFileDescriptor, blocklist: DomainBlocklist) {
        val input = FileInputStream(tun.fileDescriptor)
        val output = FileOutputStream(tun.fileDescriptor)
        val buffer = ByteArray(MAX_PACKET)
        val poll = StructPollfd().apply {
            fd = tun.fileDescriptor
            events = OsConstants.POLLIN.toShort()
        }
        while (currentCoroutineContext().isActive) {
            // Wake up now and then so cancelling the loop doesn't wait on a read that never returns.
            val length = try {
                if (Os.poll(arrayOf(poll), POLL_TIMEOUT_MS) == 0) continue
                input.read(buffer)
            } catch (e: ErrnoException) {
                break // Tunnel closed.
            } catch (e: IOException) {
                break
            }
            if (length <= 0) continue
            val query = DnsPacket.parse(buffer, length) ?: continue
            if (blocklist.isBlocked(query.name)) {
                write(output, DnsPacket.reply(query, DnsPacket.nxDomain(query.dns)))
            } else {
                scope.launch(forwarding) {
                    forward(query.dns)?.let { write(output, DnsPacket.reply(query, it)) }
                }
            }
        }
    }

    /**
     * Asks the upstream resolvers in turn; null if none answered in time. Some networks block or
     * intercept Cloudflare, so the network's own DNS is the fallback: the bundled list still
     * filters adult sites then.
     */
    private fun forward(dns: ByteArray): ByteArray? {
        val servers = (listOfNotNull(preferredUpstream) + FAMILY_DNS + networkDns()).distinct()
        for (server in servers) {
            try {
                DatagramSocket().use { socket ->
                    // Keeps the upstream query out of our own tunnel.
                    protect(socket)
                    socket.soTimeout = UPSTREAM_TIMEOUT_MS
                    socket.send(DatagramPacket(dns, dns.size, server, 53))
                    val response = DatagramPacket(ByteArray(MAX_PACKET), MAX_PACKET)
                    socket.receive(response)
                    if (preferredUpstream != server) Log.i(TAG, "Using DNS server $server")
                    preferredUpstream = server
                    return response.data.copyOf(response.length)
                }
            } catch (e: SocketTimeoutException) {
                Log.w(TAG, "DNS server $server didn't answer")
            } catch (e: IOException) {
                Log.w(TAG, "DNS server $server failed", e)
            }
        }
        Log.w(TAG, "No DNS server answered; tried $servers")
        return null
    }

    /** DNS servers of the real (non-VPN) networks, e.g. your Wi-Fi router or carrier. */
    private fun networkDns(): List<InetAddress> = try {
        networkDnsOrThrow()
    } catch (e: SecurityException) {
        Log.w(TAG, "Can't read the network's DNS servers", e)
        emptyList()
    }

    private fun networkDnsOrThrow(): List<InetAddress> {
        val connectivity = getSystemService(ConnectivityManager::class.java)
        @Suppress("DEPRECATION") // No replacement lists non-default networks for a VPN app.
        return connectivity.allNetworks
            .filter { network ->
                connectivity.getNetworkCapabilities(network)?.let {
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        !it.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                } == true
            }
            .flatMap { connectivity.getLinkProperties(it)?.dnsServers.orEmpty() }
    }

    private fun write(output: FileOutputStream, packet: ByteArray) {
        try {
            synchronized(output) { output.write(packet) }
        } catch (e: IOException) {
            // Tunnel closed while the answer was on its way; the app will retry.
        }
    }

    /** Stops the filter right when a requested turn-off's cooldown ends. */
    private fun watchCooldown() {
        cooldownJob?.cancel()
        cooldownJob = scope.launch {
            settings.state.collectLatest { state ->
                val disableAt = state.disableAt ?: return@collectLatest
                delay((disableAt - System.currentTimeMillis()).coerceAtLeast(0))
                if (!settings.settle()) shutDown()
            }
        }
    }

    override fun onRevoke() {
        // The user turned the VPN off in system settings, or another VPN app took over.
        if (settings.state.value.isActive(System.currentTimeMillis())) notifyRevoked()
        shutDown()
    }

    private fun shutDown() {
        _running.value = false
        loopJob?.cancel()
        loopJob = null
        try {
            tunnel?.close()
        } catch (e: IOException) {
            // Already closed.
        }
        tunnel = null
        stopSelf()
    }

    override fun onDestroy() {
        shutDown()
        scope.cancel()
        super.onDestroy()
    }

    private fun notifyRevoked() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Web filter", NotificationManager.IMPORTANCE_HIGH)
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Web filter is off")
            .setContentText("Adult sites aren't being blocked. Tap to turn the filter back on.")
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun openAppIntent(): PendingIntent? =
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

    companion object {
        private const val TAG = "WebFilterVpn"
        private const val CHANNEL_ID = "web_filter"
        private const val NOTIFICATION_ID = 7301

        private const val TUN_V4 = "10.111.222.1"
        private const val DNS_V4 = "10.111.222.2"
        private const val TUN_V6 = "fd00:b7a::1"
        private const val DNS_V6 = "fd00:b7a::2"

        /** Cloudflare for Families: blocks malware and adult content. */
        private val FAMILY_DNS = listOf("1.1.1.3", "1.0.0.3").map(InetAddress::getByName)
        private const val UPSTREAM_TIMEOUT_MS = 2_000
        private const val MAX_PACKET = 32_767
        private const val MAX_IN_FLIGHT = 32
        private const val POLL_TIMEOUT_MS = 1_000

        private val _running = MutableStateFlow(false)

        /** Whether the tunnel is up in this process. */
        val running: StateFlow<Boolean> = _running.asStateFlow()

        /** Null when the user has already allowed BTR to set up a VPN; otherwise the consent screen. */
        fun consentIntent(context: Context): Intent? = prepare(context)

        fun start(context: Context) {
            context.startService(Intent(context, WebFilterVpnService::class.java))
        }

        /** True when Android's Private DNS is set to a specific provider, which sends lookups around this filter. */
        fun privateDnsBypasses(context: Context): Boolean =
            Settings.Global.getString(context.contentResolver, "private_dns_mode") == "hostname"
    }
}
