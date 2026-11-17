package com.bookstore.cart;

import com.bookstore.IntegrationTestSupport;
import com.bookstore.model.Book;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartFlowTest extends IntegrationTestSupport {

    @Test
    void addUpdateAndRemoveFlow() throws Exception {
        String token = registerUser();
        Book book = createBook("19.99", 3);

        MvcResult added = mvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", idempotencyKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemBody(book.getId(), 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.total").value(39.98))
                .andReturn();

        assertThat(bookRepository.findById(book.getId()).orElseThrow().getStock()).isEqualTo(3);

        long itemId = objectMapper.readTree(added.getResponse().getContentAsString())
                .get("items").get(0).get("id").asLong();

        mvc.perform(patch("/api/cart/items/" + itemId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(3))
                .andExpect(jsonPath("$.total").value(59.97));

        assertThat(bookRepository.findById(book.getId()).orElseThrow().getStock()).isEqualTo(3);

        mvc.perform(delete("/api/cart/items/" + itemId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(0));

        assertThat(bookRepository.findById(book.getId()).orElseThrow().getStock()).isEqualTo(3);
    }

    @Test
    void addIsIdempotentForTheSameKey() throws Exception {
        String token = registerUser();
        Book book = createBook("12.50", 5);
        String key = idempotencyKey();

        MvcResult first = mvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemBody(book.getId(), 1)))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult replay = mvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemBody(book.getId(), 1)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andReturn();

        assertThat(replay.getResponse().getContentAsString())
                .isEqualTo(first.getResponse().getContentAsString());

        mvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(1));
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getStock()).isEqualTo(5);

        mvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemBody(book.getId(), 2)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void idempotencyKeyIsRequiredForCartMutations() throws Exception {
        String token = registerUser();
        Book book = createBook("9.99", 2);

        mvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemBody(book.getId(), 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
    }

    @Test
    void cannotReserveMoreThanAvailableStock() throws Exception {
        String token = registerUser();
        Book book = createBook("15.00", 1);

        mvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", idempotencyKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemBody(book.getId(), 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));

        assertThat(bookRepository.findById(book.getId()).orElseThrow().getStock()).isEqualTo(1);
    }

    @Test
    void checkoutCreatesOrderAndEmptiesCart() throws Exception {
        String token = registerUser();
        Book book = createBook("24.00", 4);
        String addKey = idempotencyKey();

        MvcResult added = mvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", addKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemBody(book.getId(), 2)))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(added.getResponse().getStatus()).isEqualTo(201);

        String checkoutKey = idempotencyKey();
        MvcResult ordered = mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", checkoutKey))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.total").value(48.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andReturn();

        String orderNumber = objectMapper.readTree(ordered.getResponse().getContentAsString())
                .get("orderNumber").asText();
        assertThat(orderNumber).startsWith("ORD-");

        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", checkoutKey))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"));

        mvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(0));

        mvc.perform(get("/api/orders").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value(orderNumber));

        assertThat(bookRepository.findById(book.getId()).orElseThrow().getStock()).isEqualTo(2);

        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", idempotencyKey()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CART_EMPTY"));
    }

    @Test
    void removeIsIdempotent() throws Exception {
        String token = registerUser();
        Book book = createBook("11.00", 2);

        mvc.perform(delete("/api/cart/items/999999")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(0));

        assertThat(bookRepository.findById(book.getId()).orElseThrow().getStock()).isEqualTo(2);
    }

    private String addItemBody(Long bookId, int quantity) {
        return "{\"bookId\":" + bookId + ",\"quantity\":" + quantity + "}";
    }
}
