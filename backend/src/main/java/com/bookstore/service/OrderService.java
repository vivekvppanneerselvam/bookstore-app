package com.bookstore.service;

import com.bookstore.common.ApiException;
import com.bookstore.dto.OrderDtos.OrderResponse;
import com.bookstore.model.Book;
import com.bookstore.model.Cart;
import com.bookstore.model.CartItem;
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CartRepository carts;
    private final OrderRepository orders;
    private final BookRepository books;
    private final UserRepository users;
    private final BookService bookService;

    public OrderService(CartRepository carts, OrderRepository orders, BookRepository books, UserRepository users,
                        BookService bookService) {
        this.carts = carts;
        this.orders = orders;
        this.books = books;
        this.users = users;
        this.bookService = bookService;
    }

    /**
     * Checkout is the only place stock is reserved. Books are locked in id order so concurrent
     * checkouts cannot oversell, and two checkouts cannot deadlock each other.
     */
    @Transactional
    public OrderResponse checkout(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("USER_NOT_FOUND", "User no longer exists"));
        Cart cart = carts.findByUserIdForUpdate(userId)
                .orElseThrow(() -> ApiException.conflict("CART_EMPTY", "Your cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw ApiException.conflict("CART_EMPTY", "Your cart is empty");
        }

        List<CartItem> items = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getBook().getId()))
                .toList();

        Order order = new Order(nextOrderNumber(), user);
        for (CartItem item : items) {
            Book book = books.findByIdForUpdate(item.getBook().getId())
                    .orElseThrow(() -> ApiException.notFound("BOOK_NOT_FOUND", "Book not found"));
            if (book.getStock() < item.getQuantity()) {
                throw ApiException.conflict("OUT_OF_STOCK",
                        "\"" + book.getTitle() + "\" only has " + book.getStock() + " copies left");
            }
            book.reserve(item.getQuantity());
            order.addItem(new OrderItem(book, item.getQuantity()));
        }

        orders.save(order);
        cart.clear();
        carts.save(cart);
        bookService.evictCatalog();
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(Long userId) {
        return orders.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        return orders.findByIdAndUserId(orderId, userId)
                .map(OrderResponse::from)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found"));
    }

    private String nextOrderNumber() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD-" + LocalDate.now(ZoneOffset.UTC).format(DATE) + "-" + suffix;
    }
}
