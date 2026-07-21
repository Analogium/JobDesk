package com.jobdesk.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpTest {

    /**
     * En production, la dernière entrée de X-Forwarded-For est l'edge Cloudflare, qui
     * change à chaque requête : s'en servir rendrait tout comptage par IP inopérant.
     */
    @Test
    void prefersTheCloudflareHeaderOverForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "203.0.113.7");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 172.68.42.11");
        request.setRemoteAddr("10.0.0.5");

        assertThat(ClientIp.of(request)).isEqualTo("203.0.113.7");
    }

    /** Sans Cloudflare : dernière entrée, la seule que le client ne peut pas falsifier. */
    @Test
    void fallsBackToTheLastForwardedEntry() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.7");

        assertThat(ClientIp.of(request)).isEqualTo("203.0.113.7");
    }

    /** Une valeur forgée par le client est ignorée au profit de celle vue par le proxy. */
    @Test
    void ignoresAClientSuppliedForwardedValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "9.9.9.9, 203.0.113.7");

        assertThat(ClientIp.of(request)).isNotEqualTo("9.9.9.9");
    }

    @Test
    void fallsBackToTheSocketAddressWhenThereIsNoProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");

        assertThat(ClientIp.of(request)).isEqualTo("10.0.0.5");
    }
}
