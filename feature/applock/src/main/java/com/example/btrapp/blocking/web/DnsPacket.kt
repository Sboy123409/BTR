package com.example.btrapp.blocking.web

/** A DNS query read off the VPN tunnel: the IP/UDP addressing plus the DNS message itself. */
class DnsQuery(
    val ipVersion: Int,
    val source: ByteArray,
    val destination: ByteArray,
    val sourcePort: Int,
    val destinationPort: Int,
    val dns: ByteArray,
    val name: String,
)

/**
 * Just enough IPv4/IPv6 + UDP + DNS handling for a DNS-only tunnel. Packets that aren't a UDP
 * DNS query with one readable question are ignored (parse returns null).
 */
object DnsPacket {

    private const val UDP = 17
    private const val DNS_PORT = 53
    private const val DNS_HEADER = 12
    private const val UDP_HEADER = 8
    private const val IPV4_HEADER = 20
    private const val IPV6_HEADER = 40

    fun parse(packet: ByteArray, length: Int = packet.size): DnsQuery? {
        if (length < 1) return null
        val version = (packet[0].toInt() shr 4) and 0xF
        val (udpStart, source, destination) = when (version) {
            4 -> {
                if (length < IPV4_HEADER) return null
                val headerLength = (packet[0].toInt() and 0xF) * 4
                if (packet[9].toInt() and 0xFF != UDP) return null
                Triple(headerLength, packet.copyOfRange(12, 16), packet.copyOfRange(16, 20))
            }
            6 -> {
                if (length < IPV6_HEADER) return null
                // Extension headers never appear on these local DNS packets.
                if (packet[6].toInt() and 0xFF != UDP) return null
                Triple(IPV6_HEADER, packet.copyOfRange(8, 24), packet.copyOfRange(24, 40))
            }
            else -> return null
        }
        if (length < udpStart + UDP_HEADER + DNS_HEADER) return null
        val sourcePort = u16(packet, udpStart)
        val destinationPort = u16(packet, udpStart + 2)
        if (destinationPort != DNS_PORT) return null
        val udpLength = u16(packet, udpStart + 4).coerceAtMost(length - udpStart)
        val dns = packet.copyOfRange(udpStart + UDP_HEADER, udpStart + udpLength)
        // Must be a query (QR = 0) with at least one question.
        if (dns.size < DNS_HEADER || dns[2].toInt() and 0x80 != 0 || u16(dns, 4) == 0) return null
        val name = questionName(dns) ?: return null
        return DnsQuery(version, source, destination, sourcePort, destinationPort, dns, name)
    }

    /** The first question's name, lowercased, or null if it's malformed. */
    fun questionName(dns: ByteArray): String? = questionEnd(dns)?.let { (name, _) -> name }

    /** An NXDOMAIN answer to [query]'s DNS message: same id and question, no records. */
    fun nxDomain(query: ByteArray): ByteArray {
        val (_, end) = questionEnd(query) ?: error("not a DNS query")
        return query.copyOfRange(0, end).also {
            it[2] = (0x80 or (query[2].toInt() and 0x79)).toByte() // QR, keep opcode and RD.
            it[3] = (0x80 or 3).toByte() // RA, RCODE 3 = NXDOMAIN.
            put16(it, 4, 1)
            put16(it, 6, 0)
            put16(it, 8, 0)
            put16(it, 10, 0)
        }
    }

    /** Wraps [dnsResponse] in an IP/UDP packet going back from the fake DNS server to the asker. */
    fun reply(query: DnsQuery, dnsResponse: ByteArray): ByteArray {
        val udpLength = UDP_HEADER + dnsResponse.size
        val ipHeader = if (query.ipVersion == 4) IPV4_HEADER else IPV6_HEADER
        val packet = ByteArray(ipHeader + udpLength)
        val from = query.destination
        val to = query.source
        if (query.ipVersion == 4) {
            packet[0] = 0x45
            put16(packet, 2, packet.size)
            packet[8] = 64 // TTL
            packet[9] = UDP.toByte()
            from.copyInto(packet, 12)
            to.copyInto(packet, 16)
            put16(packet, 10, checksum(packet, 0, IPV4_HEADER))
        } else {
            packet[0] = 0x60
            put16(packet, 4, udpLength)
            packet[6] = UDP.toByte()
            packet[7] = 64 // Hop limit
            from.copyInto(packet, 8)
            to.copyInto(packet, 24)
        }
        put16(packet, ipHeader, query.destinationPort)
        put16(packet, ipHeader + 2, query.sourcePort)
        put16(packet, ipHeader + 4, udpLength)
        dnsResponse.copyInto(packet, ipHeader + UDP_HEADER)
        put16(packet, ipHeader + 6, udpChecksum(from, to, packet, ipHeader, udpLength))
        return packet
    }

    /** Ones' complement checksum over the UDP pseudo-header and datagram (required for IPv6). */
    fun udpChecksum(from: ByteArray, to: ByteArray, packet: ByteArray, offset: Int, length: Int): Int {
        val pseudo = ByteArray(from.size + to.size + 4)
        from.copyInto(pseudo, 0)
        to.copyInto(pseudo, from.size)
        pseudo[pseudo.size - 3] = UDP.toByte()
        put16(pseudo, pseudo.size - 2, length)
        val sum = sum16(pseudo, 0, pseudo.size) + sum16(packet, offset, length)
        // A computed zero is sent as all ones; zero means "no checksum".
        return fold(sum).inv().and(0xFFFF).takeIf { it != 0 } ?: 0xFFFF
    }

    fun checksum(data: ByteArray, offset: Int, length: Int): Int =
        fold(sum16(data, offset, length)).inv() and 0xFFFF

    private fun sum16(data: ByteArray, offset: Int, length: Int): Long {
        var sum = 0L
        var i = offset
        while (i + 1 < offset + length) {
            sum += u16(data, i)
            i += 2
        }
        if (i < offset + length) sum += (data[i].toInt() and 0xFF) shl 8
        return sum
    }

    private fun fold(value: Long): Int {
        var sum = value
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.toInt()
    }

    /** The first question's name and the offset just past that question (its type and class). */
    private fun questionEnd(dns: ByteArray): Pair<String, Int>? {
        if (dns.size < DNS_HEADER) return null
        val labels = mutableListOf<String>()
        var i = DNS_HEADER
        while (true) {
            if (i >= dns.size) return null
            val len = dns[i].toInt() and 0xFF
            if (len == 0) break
            // Compression pointers don't belong in a question; reject them.
            if (len > 63 || i + 1 + len > dns.size) return null
            labels += String(dns, i + 1, len, Charsets.US_ASCII).lowercase()
            i += 1 + len
        }
        val end = i + 1 + 4
        if (end > dns.size || labels.isEmpty()) return null
        return labels.joinToString(".") to end
    }

    private fun u16(data: ByteArray, at: Int): Int =
        ((data[at].toInt() and 0xFF) shl 8) or (data[at + 1].toInt() and 0xFF)

    private fun put16(data: ByteArray, at: Int, value: Int) {
        data[at] = (value shr 8).toByte()
        data[at + 1] = value.toByte()
    }
}
