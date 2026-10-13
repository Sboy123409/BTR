package com.example.btrapp.blocking.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainBlocklistTest {

    private val list = DomainBlocklist.parse(
        """
        # comment
        adult.example

        Other.Example  # trailing comment
        """.trimIndent().reader()
    )

    @Test
    fun `blocks listed domains and their subdomains`() {
        assertTrue(list.isBlocked("adult.example"))
        assertTrue(list.isBlocked("www.adult.example"))
        assertTrue(list.isBlocked("a.b.ADULT.example."))
        assertTrue(list.isBlocked("other.example"))
    }

    @Test
    fun `does not block parents or lookalikes`() {
        assertFalse(list.isBlocked("example"))
        assertFalse(list.isBlocked("notadult.example"))
        assertFalse(list.isBlocked("adult.example.com"))
    }

    @Test
    fun `always blocks encrypted DNS endpoints and the Firefox canary`() {
        assertTrue(list.isBlocked("use-application-dns.net"))
        assertTrue(list.isBlocked("dns.google"))
        assertTrue(list.isBlocked("mozilla.cloudflare-dns.com"))
        assertFalse(list.isBlocked("google.com"))
    }
}
