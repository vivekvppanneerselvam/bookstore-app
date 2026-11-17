package com.bookstore.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 120)
    private String author;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(length = 1000)
    private String description;

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Book() {
    }

    public Book(String title, String author, String isbn, BigDecimal price, int stock, String description) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.price = price;
        this.stock = stock;
        this.description = description;
    }

    /** Takes copies out of stock once an order is placed. */
    public void reserve(int quantity) {
        if (quantity < 1 || quantity > stock) {
            throw new IllegalArgumentException("Cannot reserve " + quantity + " of stock " + stock);
        }
        stock -= quantity;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
