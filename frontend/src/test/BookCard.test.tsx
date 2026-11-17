import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { BookCard } from '../components/BookCard';
import type { Book } from '../api/types';

const book: Book = {
  id: 1,
  title: 'Clean Code',
  author: 'Robert C. Martin',
  isbn: '9780132350884',
  price: 42.9,
  stock: 3,
  description: 'A handbook of agile software craftsmanship.',
};

describe('BookCard', () => {
  it('renders book details and triggers add to cart', async () => {
    const onAdd = vi.fn();
    render(<BookCard book={book} onAdd={onAdd} />);

    expect(screen.getByText('Clean Code')).toBeInTheDocument();
    expect(screen.getByText('Robert C. Martin')).toBeInTheDocument();
    expect(screen.getByText('$42.90')).toBeInTheDocument();
    expect(screen.getByText('3 in stock')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Add to cart' }));
    expect(onAdd).toHaveBeenCalledTimes(1);
  });

  it('disables the action when the book is out of stock', () => {
    render(<BookCard book={{ ...book, stock: 0 }} onAdd={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Out of stock' })).toBeDisabled();
  });
});
