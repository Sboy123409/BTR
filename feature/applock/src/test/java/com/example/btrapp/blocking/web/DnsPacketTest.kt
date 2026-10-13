package com.example.btrapp.blocking.web

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DnsPacketTest {

    private val phone = byteArrayOf(10, 111, 222.toByte(), 1)
    private val fakeDns = byteArrayOf(10, 111, 222.toByte(), 2)

    /** A standard query for [name], type A, recursion desired. */
    private fun dnsQuery(name: String, id: Int = 0x1234): ByteArray {
        val question = name.split('.').flatMap { listOf(it.length.toByte()) + it.toByteArray().toList() } +
            listOf<Byte>(0, 0, 1, 0, 1)
        val header = listOf(id shr 8, id, 0x01, 0x00, 0, 1, 0, 0, 0, 0, 0, 0).map { it.toByte() }
        return (header + question).toByteArray()
    }

    private fun ipv4Udp(dns: ByteArray, sourcePort: Int = 40000, destinationPort: Int = 53): ByteArray {
        val packet = ByteArray(20 + 8 + dns.size)
        packet[0] = 0x45
        packet[2] = (packet.size shr 8).toByte()
        packet[3] = packet.size.toByte()
        packet[8] = 64
        packet[9] = 17
        phone.copyInto(packet, 12)
        fakeDns.copyInto(packet, 16)
        packet[20] = (sourcePort shr 8).toByte()
        packet[21] = sourcePort.toByte()
        packet[22] = (destinationPort shr 8).toByte()
        packet[23] = destinationPort.toByte()
        packet[24] = ((8 + dns.size) shr 8).toByte()
        packet[25] = (8 + dns.size).toByte()
        dns.copyInto(packet, 28)
        return packet
    }

    /** Ones' complement sum; a correctly checksummed block sums to 0xFFFF. */
    private fun onesSum(vararg parts: ByteArray): Int {
        var sum = 0L
        parts.forEach { part ->
            for (i in part.indices step 2) {
                val hi = part[i].toInt() and 0xFF
                val lo = if (i + 1 < part.size) part[i + 1].toInt() and 0xFF else 0
                sum += (hi shl 8) or lo
            }
        }
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.toInt()
    }

    @Test
    fun `parses an IPv4 DNS query`() {
        val query = DnsPacket.parse(ipv4Udp(dnsQuery("WWW.Example.com")))!!
        assertEquals("www.example.com", query.name)
        assertEquals(4, query.ipVersion)
        assertEquals(40000, query.sourcePort)
        assertArrayEquals(phone, query.source)
        assertArrayEquals(fakeDns, query.destination)
    }

    @Test
    fun `ignores non-DNS and malformed packets`() {
        assertNull(DnsPacket.parse(ipv4Udp(dnsQuery("example.com"), destinationPort = 443)))
        assertNull(DnsPacket.parse(ipv4Udp(dnsQuery("example.com")).copyOf(30)))
        assertNull(DnsPacket.parse(ByteArray(0)))
        val response = dnsQuery("example.com").also { it[2] = 0x81.toByte() }
        assertNull(DnsPacket.parse(ipv4Udp(response)))
    }

    @Test
    fun `nxdomain keeps id and question and sets rcode`() {
        val query = dnsQuery("blocked.example", id = 0xBEEF)
        val answer = DnsPacket.nxDomain(query)
        assertEquals(0xBE.toByte(), answer[0])
        assertEquals(0xEF.toByte(), answer[1])
        assertEquals(0x81, answer[2].toInt() and 0xFF) // QR + RD
        assertEquals(3, answer[3].toInt() and 0x0F)
        assertEquals("blocked.example", DnsPacket.questionName(answer))
        assertEquals(query.size, answer.size)
    }

    @Test
    fun `reply swaps addresses and has valid checksums`() {
        val query = DnsPacket.parse(ipv4Udp(dnsQuery("example.com")))!!
        val dns = DnsPacket.nxDomain(query.dns)
        val packet = DnsPacket.reply(query, dns)

        assertArrayEquals(fakeDns, packet.copyOfRange(12, 16))
        assertArrayEquals(phone, packet.copyOfRange(16, 20))
        assertEquals(53, ((packet[20].toInt() and 0xFF) shl 8) or (packet[21].toInt() and 0xFF))
        assertEquals(0xFFFF, onesSum(packet.copyOfRange(0, 20)))

        val udp = packet.copyOfRange(20, packet.size)
        val pseudo = fakeDns + phone + byteArrayOf(0, 17, (udp.size shr 8).toByte(), udp.size.toByte())
        assertEquals(0xFFFF, onesSum(pseudo, udp))
        assertArrayEquals(dns, packet.copyOfRange(28, packet.size))
    }

    @Test
    fun `reply over IPv6 has a valid UDP checksum`() {
        val from = ByteArray(16).also { it[0] = 0xFD.toByte(); it[15] = 2 }
        val to = ByteArray(16).also { it[0] = 0xFD.toByte(); it[15] = 1 }
        val query = DnsQuery(6, to, from, 40000, 53, dnsQuery("example.com"), "example.com")
        val packet = DnsPacket.reply(query, DnsPacket.nxDomain(query.dns))

        assertEquals(0x60, packet[0].toInt() and 0xF0)
        val udp = packet.copyOfRange(40, packet.size)
        val pseudo = from + to + byteArrayOf(0, 0, (udp.size shr 8).toByte(), udp.size.toByte(), 0, 0, 0, 17)
        assertEquals(0xFFFF, onesSum(pseudo, udp))
        // And it parses back as coming from the fake server.
        assertArrayEquals(from, packet.copyOfRange(8, 24))
    }
}
