package com.icms.controller;

import com.icms.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthIntegrationIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void loginWithValidCredentialsReturnsTokenAndSanitizedUserInfo() {
        String body = """
                {"username":"admin","password":"123456","captchaId":"","captchaCode":""}""";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                request,
                Map.class);

        assertEquals(200, response.getStatusCodeValue());

        Map<String, Object> result = response.getBody();
        assertNotNull(result);
        assertEquals(200, result.get("code"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertNotNull(data);
        assertNotNull(data.get("token"));
        assertTrue(data.get("token").toString().length() > 10);

        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) data.get("userInfo");
        assertNotNull(userInfo);
        assertEquals("admin", userInfo.get("username"));
        assertNull(userInfo.get("password"));
        assertNull(userInfo.get("refresh_token"));
        assertNull(userInfo.get("refreshToken"));
    }

    @Test
    void loginWithWrongPasswordReturnsError() {
        String body = """
                {"username":"admin","password":"wrong","captchaId":"","captchaCode":""}""";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                request,
                Map.class);

        assertEquals(200, response.getStatusCodeValue());

        Map<String, Object> result = response.getBody();
        assertNotNull(result);
        // business error, HTTP 200 but code != 200
        assertNotEquals(200, result.get("code"));
    }
}
