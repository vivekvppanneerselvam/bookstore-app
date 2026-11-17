package com.bookstore;

import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected BookRepository bookRepository;

    protected MvcResult registerUserResult() throws Exception {
        return registerUserResult("password123");
    }

    protected MvcResult registerUserResult(String password) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        return mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Registration(email, password, "Test Reader"))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    protected String registerUser() throws Exception {
        return accessToken(registerUserResult());
    }

    protected String accessToken(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    protected Book createBook(String price, int stock) {
        String isbn = "978" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 10);
        return bookRepository.save(new Book("Test Book " + isbn, "Test Author", isbn, new BigDecimal(price), stock,
                "A book created for tests"));
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String idempotencyKey() {
        return UUID.randomUUID().toString();
    }

    protected String refreshCookieValue(MvcResult result) {
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        if (header == null) {
            return null;
        }
        String firstSegment = header.split(";", 2)[0];
        int equals = firstSegment.indexOf('=');
        return equals < 0 ? null : firstSegment.substring(equals + 1);
    }

    protected Cookie refreshCookie(String value) {
        return new Cookie("refresh_token", value);
    }

    public record Registration(String email, String password, String displayName) {
    }
}
