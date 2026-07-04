package com.icms.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestClientUtilTest {

    @Test
    void fallsBackToRemoteAddrWithoutProxyHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertEquals("127.0.0.1", RequestClientUtil.getClientIp(request));
    }

    @Test
    void usesCloudflareHeaderBeforeOtherProxyHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("CF-Connecting-IP", "203.0.113.10");
        request.addHeader("X-Real-IP", "10.0.0.5");
        request.addHeader("X-Forwarded-For", "198.51.100.20, 127.0.0.1");

        assertEquals("203.0.113.10", RequestClientUtil.getClientIp(request));
    }

    @Test
    void usesFirstForwardedForAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.20, 127.0.0.1");

        assertEquals("198.51.100.20", RequestClientUtil.getClientIp(request));
    }

    @Test
    void parsesForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("Forwarded", "for=198.51.100.30;proto=https;host=icms.example");

        assertEquals("198.51.100.30", RequestClientUtil.getClientIp(request));
    }

    @Test
    void ignoresUnknownHeaderValues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("CF-Connecting-IP", "unknown");
        request.addHeader("X-Forwarded-For", " unknown, 198.51.100.40");

        assertEquals("198.51.100.40", RequestClientUtil.getClientIp(request));
    }

    @Test
    void ignoresInvalidHeaderValues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("CF-Connecting-IP", "not-an-ip");
        request.addHeader("X-Forwarded-For", "999.999.999.999, 198.51.100.50");

        assertEquals("198.51.100.50", RequestClientUtil.getClientIp(request));
    }
}
