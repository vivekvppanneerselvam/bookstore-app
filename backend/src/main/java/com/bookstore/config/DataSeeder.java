package com.bookstore.config;

import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import com.bookstore.service.BookService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final BookRepository books;
    private final BookService bookService;

    public DataSeeder(BookRepository books, BookService bookService) {
        this.books = books;
        this.bookService = bookService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (books.count() > 0) {
            return;
        }
        books.saveAll(List.of(
                new Book("Clean Code", "Robert C. Martin", "9780132350884", new BigDecimal("42.90"), 12,
                        "A handbook of agile software craftsmanship."),
                new Book("The Pragmatic Programmer", "Andrew Hunt, David Thomas", "9780135957059",
                        new BigDecimal("49.99"), 8, "Your journey to mastery, 20th anniversary edition."),
                new Book("Design Patterns", "Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides",
                        "9780201633610", new BigDecimal("54.50"), 5,
                        "Elements of reusable object-oriented software."),
                new Book("Refactoring", "Martin Fowler", "9780134757599", new BigDecimal("47.25"), 7,
                        "Improving the design of existing code."),
                new Book("Effective Java", "Joshua Bloch", "9780134685991", new BigDecimal("45.00"), 10,
                        "Best practices for the Java platform."),
                new Book("Domain-Driven Design", "Eric Evans", "9780321125217", new BigDecimal("58.75"), 4,
                        "Tackling complexity in the heart of software."),
                new Book("The Mythical Man-Month", "Frederick P. Brooks", "9780201835953",
                        new BigDecimal("34.99"), 6, "Essays on software engineering."),
                new Book("Working Effectively with Legacy Code", "Michael C. Feathers", "9780131177055",
                        new BigDecimal("51.10"), 3, "Techniques for bringing legacy systems under control."),
                new Book("Continuous Delivery", "Jez Humble, David Farley", "9780321601919",
                        new BigDecimal("56.40"), 9, "Reliable software releases through build, test and deployment automation."),
                new Book("You Don't Know JS Yet", "Kyle Simpson", "9781098124042", new BigDecimal("29.99"), 15,
                        "Get started with the JavaScript language.")
        ));
        bookService.evictCatalog();
    }
}
