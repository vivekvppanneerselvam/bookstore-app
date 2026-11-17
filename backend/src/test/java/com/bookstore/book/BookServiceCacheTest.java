package com.bookstore.book;

import com.bookstore.common.ApiException;
import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import com.bookstore.service.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceCacheTest {

    @Mock
    private BookRepository books;

    @Test
    void servesLastKnownCatalogWhenDatabaseGoesDown() {
        Book book = new Book("Clean Code", "Robert C. Martin", "9780132350884", new BigDecimal("42.90"), 5, null);
        when(books.findAllByOrderByTitleAsc())
                .thenReturn(List.of(book))
                .thenThrow(new DataAccessResourceFailureException("database is down"));

        BookService service = new BookService(books, 60);

        BookService.CatalogResult fresh = service.getCatalog();
        assertThat(fresh.cacheStatus()).isEqualTo(BookService.CacheStatus.MISS);
        assertThat(fresh.books()).hasSize(1);

        service.evictCatalog();
        BookService.CatalogResult stale = service.getCatalog();
        assertThat(stale.cacheStatus()).isEqualTo(BookService.CacheStatus.STALE);
        assertThat(stale.books()).hasSize(1);
    }

    @Test
    void returnsServiceUnavailableWhenNoSnapshotExists() {
        when(books.findAllByOrderByTitleAsc()).thenThrow(new DataAccessResourceFailureException("database is down"));

        BookService service = new BookService(books, 60);

        assertThatThrownBy(service::getCatalog)
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
