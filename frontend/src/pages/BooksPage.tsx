import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../api/client';
import { useBooks } from '../api/queries';
import type { Book, Cart } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { BookCard } from '../components/BookCard';
import { newIdempotencyKey } from '../utils/idempotency';

export function BooksPage() {
  const { data: books, isLoading, isError, error } = useBooks();
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const addToCart = useMutation<Cart, unknown, Book>({
    mutationFn: async (book) => {
      const response = await api.post<Cart>(
        '/cart/items',
        { bookId: book.id, quantity: 1 },
        { headers: { 'Idempotency-Key': newIdempotencyKey() } },
      );
      return response.data;
    },
    onSuccess: (cart) => {
      queryClient.setQueryData(['cart'], cart);
      queryClient.invalidateQueries({ queryKey: ['books'] });
    },
  });

  useEffect(() => {
    if (!addToCart.isSuccess) {
      return;
    }
    const timer = window.setTimeout(() => addToCart.reset(), 3000);
    return () => window.clearTimeout(timer);
  }, [addToCart.isSuccess, addToCart.reset]);

  const handleAdd = (book: Book) => {
    if (!user) {
      navigate('/login', { state: { from: '/' } });
      return;
    }
    addToCart.mutate(book);
  };

  return (
    <section>
      <div className="page-heading">
        <div>
          <h1>Browse books</h1>
          <p className="muted">Stock is reserved as soon as you add a book to your cart.</p>
        </div>
      </div>

      {addToCart.isError && <div className="alert error">{getErrorMessage(addToCart.error)}</div>}
      {addToCart.isSuccess && (
        <div className="alert success">
          Added “{addToCart.variables?.title}” to your cart.
        </div>
      )}

      {isLoading && <div className="page-loading">Loading books…</div>}
      {isError && <div className="alert error">{getErrorMessage(error)}</div>}

      <div className="book-grid">
        {books?.map((book) => (
          <BookCard
            key={book.id}
            book={book}
            onAdd={() => handleAdd(book)}
            disabled={addToCart.isPending}
          />
        ))}
      </div>

      {books && books.length === 0 && <p className="muted">No books available right now.</p>}
    </section>
  );
}
