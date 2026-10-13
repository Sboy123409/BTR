package com.example.btrapp.blocking.web

import java.io.Reader

/** Blocks a domain when it, or any domain it's under, is on the list. */
class DomainBlocklist(domains: Set<String>) {

    private val domains: Set<String> = domains + ALWAYS_BLOCKED

    val size: Int get() = domains.size

    fun isBlocked(name: String): Boolean {
        var candidate = name.lowercase().trimEnd('.')
        while (true) {
            if (candidate in domains) return true
            val dot = candidate.indexOf('.')
            if (dot < 0) return false
            candidate = candidate.substring(dot + 1)
        }
    }

    companion object {
        /**
         * Encrypted-DNS (DoH) endpoints that browsers could use to skip this filter, plus Firefox's
         * canary domain: answering NXDOMAIN for it tells Firefox to turn its own DoH off.
         */
        val ALWAYS_BLOCKED = setOf(
            "use-application-dns.net",
            "dns.google",
            "dns.google.com",
            "cloudflare-dns.com",
            "mozilla.cloudflare-dns.com",
            "chrome.cloudflare-dns.com",
            "dns.quad9.net",
            "doh.opendns.com",
            "dns.adguard.com",
            "dns.adguard-dns.com",
            "dns.nextdns.io",
            "doh.cleanbrowsing.org",
            "doh.mullvad.net",
        )

        /** Reads one domain per line; blank lines and `#` comments are skipped. */
        fun parse(reader: Reader): DomainBlocklist {
            val domains = HashSet<String>(100_000)
            reader.buffered().forEachLine { line ->
                val domain = line.substringBefore('#').trim().lowercase()
                if (domain.isNotEmpty()) domains += domain
            }
            return DomainBlocklist(domains)
        }
    }
}
