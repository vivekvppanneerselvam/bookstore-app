package com.bookstore.service;

import com.bookstore.common.ApiException;
import com.bookstore.dto.CartDtos.AddItemRequest;
import com.bookstore.dto.CartDtos.CartItemResponse;
import com.bookstore.dto.CartDtos.CartResponse;
import com.bookstore.model.Book;
import com.bookstore.model.Cart;
import com.bookstore.model.CartItem;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    static final int MAX_QUANTITY_PER_BOOK = 10;

    private final CartRepository carts;
    private final BookRepository books;
    private final UserRepository users;

    public CartService(CartRepository carts, BookRepository books, UserRepository users) {
        this.carts = carts;
        this.books = books;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return toResponse(carts.findByUserId(userId).orElse(null));
    }

    @Transactional
    public CartResponse addItem(Long userId, AddItemRequest request) {
        User user = lockUser(userId);
        Cart cart = carts.findByUserId(userId).orElseGet(() -> carts.save(new Cart(user)));

        Book book = books.findById(request.bookId())
                .orElseThrow(() -> ApiException.notFound("BOOK_NOT_FOUND", "Book not found"));

        CartItem existing = cart.findItemByBook(book.getId());
        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + request.quantity();
        if (newQuantity > MAX_QUANTITY_PER_BOOK) {
            throw ApiException.badRequest("QUANTITY_LIMIT_EXCEEDED",
                    "At most " + MAX_QUANTITY_PER_BOOK + " copies of the same book per cart");
        }
        requireStock(book, newQuantity);

        cart.addItem(book, request.quantity());
        carts.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateQuantity(Long userId, Long itemId, int quantity) {
        Cart cart = carts.findByUserIdForUpdate(userId).orElse(null);
        CartItem item = cart == null ? null : findItem(cart, itemId);
        if (item == null) {
            throw ApiException.notFound("CART_ITEM_NOT_FOUND", "Cart item not found");
        }

        requireStock(item.getBook(), quantity);
        cart.changeQuantity(item, quantity);
        carts.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = carts.findByUserIdForUpdate(userId).orElse(null);
        if (cart == null) {
            return toResponse(null);
        }
        CartItem item = findItem(cart, itemId);
        if (item == null) {
            return toResponse(cart);
        }
        cart.removeItem(item);
        carts.save(cart);
        return toResponse(cart);
    }

    private User lockUser(Long userId) {
        return users.findByIdForUpdate(userId)
                .orElseThrow(() -> ApiException.unauthorized("USER_NOT_FOUND", "User no longer exists"));
    }

    private CartItem findItem(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElse(null);
    }

    private void requireStock(Book book, int quantity) {
        if (book.getStock() < quantity) {
            throw ApiException.conflict("OUT_OF_STOCK", "Only " + book.getStock() + " copies left in stock");
        }
    }

    private CartResponse toResponse(Cart cart) {
        if (cart == null) {
            return new CartResponse(null, List.of(), 0, BigDecimal.ZERO);
        }
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> {
                    Book book = item.getBook();
                    BigDecimal subtotal = book.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new CartItemResponse(item.getId(), book.getId(), book.getTitle(), book.getAuthor(),
                            book.getPrice(), item.getQuantity(), subtotal);
                })
                .toList();
        int itemCount = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, itemCount, total);
    }
}
