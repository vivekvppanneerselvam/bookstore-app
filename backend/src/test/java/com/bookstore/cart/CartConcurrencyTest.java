package com.bookstore.cart;

import com.bookstore.IntegrationTestSupport;
import com.bookstore.common.ApiException;
import com.bookstore.dto.CartDtos.AddItemRequest;
import com.bookstore.model.Book;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.CartService;
import com.bookstore.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CartConcurrencyTest extends IntegrationTestSupport {

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void onlyOneOfTwoCheckoutsCanTakeTheLastCopy() throws Exception {
        Book book = createBook("30.00", 1);
        User alice = userRepository.save(new User(uniqueEmail("alice"), "unused-hash", "Alice"));
        User bob = userRepository.save(new User(uniqueEmail("bob"), "unused-hash", "Bob"));

        cartService.addItem(alice.getId(), new AddItemRequest(book.getId(), 1));
        cartService.addItem(bob.getId(), new AddItemRequest(book.getId(), 1));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> first = pool.submit(checkout(alice.getId(), ready, start));
            Future<String> second = pool.submit(checkout(bob.getId(), ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<String> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(results).containsExactlyInAnyOrder("OK", "OUT_OF_STOCK");
            assertThat(bookRepository.findById(book.getId()).orElseThrow().getStock()).isZero();
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<String> checkout(Long userId, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                orderService.checkout(userId);
                return "OK";
            } catch (ApiException ex) {
                return ex.getCode();
            }
        };
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
