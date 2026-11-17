package com.bookstore.controller;

import com.bookstore.dto.BookDtos.BookResponse;
import com.bookstore.service.BookService;
import com.bookstore.service.BookService.CatalogResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<List<BookResponse>> list() {
        CatalogResult result = bookService.getCatalog();
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header("X-Cache", result.cacheStatus().name());
        if (result.cacheStatus() == BookService.CacheStatus.STALE) {
            builder.header("X-Data-Stale", "true");
        }
        return builder.body(result.books());
    }
}
