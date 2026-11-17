package com.bookstore.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected Cart() {
    }

    public Cart(User user) {
        this.user = user;
    }

    public CartItem addItem(Book book, int quantity) {
        CartItem item = findItemByBook(book.getId());
        if (item == null) {
            item = new CartItem(this, book);
            items.add(item);
        }
        item.setQuantity(item.getQuantity() + quantity);
        touch();
        return item;
    }

    public CartItem findItemByBook(Long bookId) {
        return items.stream()
                .filter(item -> item.getBook().getId().equals(bookId))
                .findFirst()
                .orElse(null);
    }

    public void changeQuantity(CartItem item, int quantity) {
        item.setQuantity(quantity);
        touch();
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        touch();
    }

    public void clear() {
        items.clear();
        touch();
    }

    public void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
