package com.bookstore.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_idempotency_user_key", columnNames = {"user_id", "idem_key"}))
public class IdempotencyRecord {

    public enum Status {
        IN_PROGRESS, COMPLETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "idem_key", nullable = false, length = 64)
    private String key;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.IN_PROGRESS;

    private Integer responseStatus;

    @Lob
    private String responseBody;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(Long userId, String key, String requestHash) {
        this.userId = userId;
        this.key = key;
        this.requestHash = requestHash;
    }

    public void complete(int status, String body) {
        this.status = Status.COMPLETED;
        this.responseStatus = status;
        this.responseBody = body;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getKey() {
        return key;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Status getStatus() {
        return status;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
