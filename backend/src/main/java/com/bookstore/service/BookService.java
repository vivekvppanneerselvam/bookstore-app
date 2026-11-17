package com.bookstore.service;

import com.bookstore.common.ApiException;
import com.bookstore.dto.BookDtos.BookResponse;
import com.bookstore.repository.BookRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class BookService {

    private static final String CATALOG_KEY = "catalog";

    private final BookRepository books;
    private final Cache<String, List<BookResponse>> cache;
    private final AtomicReference<List<BookResponse>> lastKnownCatalog = new AtomicReference<>();

    public BookService(BookRepository books,
                       @Value("${app.cache.books-ttl-seconds:60}") long ttlSeconds) {
        this.books = books;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(1)
                .build();
    }

    public CatalogResult getCatalog() {
        List<BookResponse> cached = cache.getIfPresent(CATALOG_KEY);
        if (cached != null) {
            return new CatalogResult(cached, CacheStatus.HIT);
        }
        try {
            List<BookResponse> fresh = books.findAllByOrderByTitleAsc().stream()
                    .map(BookResponse::from)
                    .toList();
            cache.put(CATALOG_KEY, fresh);
            lastKnownCatalog.set(fresh);
            return new CatalogResult(fresh, CacheStatus.MISS);
        } catch (DataAccessException ex) {
            List<BookResponse> stale = lastKnownCatalog.get();
            if (stale != null) {
                return new CatalogResult(stale, CacheStatus.STALE);
            }
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_UNAVAILABLE",
                    "The book catalog is temporarily unavailable", ex);
        }
    }

    public void evictCatalog() {
        cache.invalidateAll();
    }

    public enum CacheStatus {
        HIT, MISS, STALE
    }

    public record CatalogResult(List<BookResponse> books, CacheStatus cacheStatus) {
    }
}
