package com.coldcat.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun emptyInputOpensHome() {
        assertEquals(UrlUtils.HOME, UrlUtils.toUri("   "))
    }

    @Test
    fun hostGetsHttps() {
        assertEquals("https://example.com", UrlUtils.toUri("example.com"))
        assertEquals("https://www.example.com/path", UrlUtils.toUri("https://www.example.com/path"))
    }

    @Test
    fun plainTextBecomesSearch() {
        assertTrue(UrlUtils.toUri("cold cat browser").startsWith("https://duckduckgo.com/?q="))
        assertTrue(UrlUtils.toUri("hello").startsWith("https://duckduckgo.com/?q="))
    }

    @Test
    fun localhostAndIpUseHttp() {
        assertEquals("http://localhost:8080", UrlUtils.toUri("localhost:8080"))
        assertEquals("http://192.168.1.1", UrlUtils.toUri("192.168.1.1"))
    }

    @Test
    fun internalPagesPassThrough() {
        assertEquals(UrlUtils.HOME, UrlUtils.toUri(UrlUtils.HOME))
        assertEquals("about:blank", UrlUtils.toUri("about:blank"))
    }
}
