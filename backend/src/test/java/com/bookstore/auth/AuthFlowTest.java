package com.bookstore.auth;

import com.bookstore.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowTest extends IntegrationTestSupport {

    @Test
    void registerThenLoginReturnsAccessTokenAndRefreshCookie() throws Exception {
        String email = "reader-" + UUID.randomUUID() + "@example.com";

        MvcResult registered = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Registration(email, "password123", "Reader"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        assertThat(refreshCookieValue(registered)).isNotBlank();

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        MvcResult loggedIn = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        mvc.perform(get("/api/auth/me").header("Authorization", bearer(accessToken(loggedIn))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void duplicateRegistrationIsRejected() throws Exception {
        MvcResult registered = registerUserResult();
        String email = objectMapper.readTree(registered.getResponse().getContentAsString())
                .get("user").get("email").asText();

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Registration(email, "password123", "Someone Else"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void weakPasswordIsRejectedWithFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"weak@example.com\",\"password\":\"short\",\"displayName\":\"W\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void refreshTokenRotationAndReuseDetection() throws Exception {
        MvcResult registered = registerUserResult();
        String originalRefresh = refreshCookieValue(registered);
        assertThat(originalRefresh).isNotBlank();

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(refreshCookie(originalRefresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String rotated = refreshCookieValue(refreshed);
        assertThat(rotated).isNotBlank().isNotEqualTo(originalRefresh);

        mvc.perform(post("/api/auth/refresh").cookie(refreshCookie(originalRefresh)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));

        // Reuse detection revokes the whole token family, including the rotated token.
        mvc.perform(post("/api/auth/refresh").cookie(refreshCookie(rotated)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        MvcResult registered = registerUserResult();
        String refresh = refreshCookieValue(registered);
        assertThat(refresh).isNotBlank();

        mvc.perform(post("/api/auth/logout").cookie(refreshCookie(refresh)))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/refresh").cookie(refreshCookie(refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }
}
