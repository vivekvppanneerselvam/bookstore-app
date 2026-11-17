package com.bookstore.dto;

import com.bookstore.model.Book;

import java.math.BigDecimal;

public final class BookDtos {

    private BookDtos() {
    }

    public record BookResponse(Long id, String title, String author, String isbn, BigDecimal price, int stock,
                               String description) {

        public static BookResponse from(Book book) {
            return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
                    book.getPrice(), book.getStock(), book.getDescription());
        }
    }
}
