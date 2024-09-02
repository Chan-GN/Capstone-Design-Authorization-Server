package com.hansung.hansungauthorizationserver;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// OIDC 인증 흐름을 테스트하는 통합 테스트 클래스
@SpringBootTest
@AutoConfigureMockMvc
class OIDCAuthorizationFlowIntegrationTest {

    private static final String AS_URL = "http://localhost:8081";
    private static final String FE_URL = "http://localhost:8070";
    private static final String CLIENT_ID = "client";
    private static final String CLIENT_SECRET = "secret";
    private static final String TEST_USER = "1891239";
    private static final String TEST_PASSWORD = "1234";
    private static final String TEST_ROLE = "STUDENT";

    @Autowired
    private MockMvc mockMvc;

    private String codeVerifier;
    private String codeChallenge;

    // 각 테스트 실행 전 PKCE를 위한 코드 검증기와 코드 챌린지 생성
    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        codeVerifier = generateCodeVerifier();
        codeChallenge = generateCodeChallenge(codeVerifier);
    }

    // 인가 코드 발급 과정 테스트
    @Test
    @DisplayName("인가 코드 발급 확인 테스트")
    public void testAuthorizationCodeGrant() throws Exception {
        MvcResult result = performAuthorizationCodeRequest();

        String redirectedUrl = result.getResponse().getHeader("Location");
        assertThat(redirectedUrl).startsWith(FE_URL + "/authorized");
        assertThat(redirectedUrl).contains("code=");
    }

    // 전체 OIDC 인증 흐름 테스트 (인가 코드 발급, 토큰 요청 및 발급, 토큰 검증)
    @Test
    @DisplayName("토큰 요청, 발급 및 토큰 검증 테스트")
    public void testTokenRequestIssuanceAndIntrospection() throws Exception {
        // 1. 인가 코드 획득
        String code = getAuthorizationCode();

        // 2. 토큰 요청 및 발급
        JSONObject tokenResponse = requestToken(code);
        String accessToken = tokenResponse.getString("access_token");
        String idToken = tokenResponse.getString("id_token");

        // 3. 토큰 인트로스펙션
        validateToken(accessToken, "access_token");
        validateToken(idToken, "id_token");
    }

    // 인가 코드 요청을 수행하는 메소드
    private MvcResult performAuthorizationCodeRequest() throws Exception {
        return mockMvc.perform(get(AS_URL + "/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", CLIENT_ID)
                        .queryParam("scope", "openid")
                        .queryParam("redirect_uri", FE_URL + "/authorized")
                        .queryParam("code_challenge", codeChallenge)
                        .queryParam("code_challenge_method", "S256")
                        .with(user(TEST_USER).password(TEST_PASSWORD).roles(TEST_ROLE)))
                .andExpect(status().is3xxRedirection())
                .andReturn();
    }

    // 인가 코드를 획득하는 메소드
    private String getAuthorizationCode() throws Exception {
        MvcResult authResult = performAuthorizationCodeRequest();
        String redirectedUrl = authResult.getResponse().getHeader("Location");
        return extractCode(redirectedUrl);
    }

    // 토큰을 요청하고 발급받는 메소드
    private JSONObject requestToken(String code) throws Exception {
        String clientCredentials = getBasicAuthHeader(CLIENT_ID, CLIENT_SECRET);
        MvcResult tokenResult = mockMvc.perform(post(AS_URL + "/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", "Basic " + clientCredentials)
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", FE_URL + "/authorized")
                        .param("code_verifier", codeVerifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").exists())
                .andExpect(jsonPath("$.id_token").exists())
                .andReturn();

        return new JSONObject(tokenResult.getResponse().getContentAsString());
    }

    // 토큰을 검증하는 메소드
    private void validateToken(String token, String tokenType) throws Exception {
        String clientCredentials = getBasicAuthHeader(CLIENT_ID, CLIENT_SECRET);
        MvcResult result = mockMvc.perform(post(AS_URL + "/oauth2/introspect")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", "Basic " + clientCredentials)
                        .param("token", token)
                        .param("token_type_hint", tokenType))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject jsonResponse = new JSONObject(result.getResponse().getContentAsString());

        assertTrue(jsonResponse.getBoolean("active"), "Token should be active");
        assertEquals(CLIENT_ID, jsonResponse.getString("client_id"), "Client ID should match");

        if ("access_token".equals(tokenType)) {
            assertTrue(jsonResponse.getString("scope").contains("openid"), "Token should have 'openid' scope");
        } else if ("id_token".equals(tokenType)) {
            assertNotNull(jsonResponse.getString("sub"), "Subject should not be null for ID token");
        }
    }

    // PKCE를 위한 코드 검증기 생성 메소드
    private String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    // PKCE를 위한 코드 챌린지 생성 메소드
    private String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // 리다이렉트 URL에서 인가 코드를 추출하는 메소드
    private String extractCode(String redirectUrl) {
        return redirectUrl.split("code=")[1].split("&")[0];
    }

    // Basic 인증 헤더를 생성하는 메소드
    private String getBasicAuthHeader(String username, String password) {
        return Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

}