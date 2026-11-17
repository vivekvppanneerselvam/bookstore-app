import type { Book } from '../api/types';
import { formatCurrency } from '../utils/format';

interface BookCardProps {
  book: Book;
  onAdd: () => void;
  disabled?: boolean;
}

export function BookCard({ book, onAdd, disabled = false }: BookCardProps) {
  const outOfStock = book.stock <= 0;
  return (
    <article className="book-card">
      <div className="book-card-body">
        <h3>{book.title}</h3>
        <p className="muted">{book.author}</p>
        {book.description && <p className="description">{book.description}</p>}
      </div>
      <div className="book-card-meta">
        <span className="price">{formatCurrency(book.price)}</span>
        <span className={outOfStock ? 'stock out' : 'stock'}>{outOfStock ? 'Out of stock' : `${book.stock} in stock`}</span>
      </div>
      <button type="button" className="btn primary full" onClick={onAdd} disabled={disabled || outOfStock}>
        {outOfStock ? 'Out of stock' : 'Add to cart'}
      </button>
    </article>
  );
}
