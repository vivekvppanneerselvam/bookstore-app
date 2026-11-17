package com.bookstore.controller;

import com.bookstore.dto.OrderDtos.OrderResponse;
import com.bookstore.security.AuthPrincipal;
import com.bookstore.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> checkout(@AuthenticationPrincipal AuthPrincipal principal) {
        OrderResponse order = orderService.checkout(principal.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/orders/" + order.id()))
                .body(order);
    }

    @GetMapping
    public List<OrderResponse> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return orderService.listOrders(principal.id());
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long orderId) {
        return orderService.getOrder(principal.id(), orderId);
    }
}
