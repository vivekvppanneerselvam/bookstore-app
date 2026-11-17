import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { CartItemRow } from '../components/CartItemRow';
import type { CartItem } from '../api/types';

const item: CartItem = {
  id: 7,
  bookId: 1,
  title: 'Clean Code',
  author: 'Robert C. Martin',
  unitPrice: 20,
  quantity: 2,
  subtotal: 40,
};

function renderRow(overrides: Partial<CartItem> = {}) {
  const onQuantityChange = vi.fn();
  const onRemove = vi.fn();
  render(
    <table>
      <tbody>
        <CartItemRow
          item={{ ...item, ...overrides }}
          onQuantityChange={onQuantityChange}
          onRemove={onRemove}
        />
      </tbody>
    </table>,
  );
  return { onQuantityChange, onRemove };
}

describe('CartItemRow', () => {
  it('updates the quantity when the buttons are used', async () => {
    const { onQuantityChange } = renderRow();

    await userEvent.click(screen.getByRole('button', { name: 'Increase quantity of Clean Code' }));
    expect(onQuantityChange).toHaveBeenCalledWith(3);

    await userEvent.click(screen.getByRole('button', { name: 'Decrease quantity of Clean Code' }));
    expect(onQuantityChange).toHaveBeenCalledWith(1);
  });

  it('blocks going below one copy', () => {
    renderRow({ quantity: 1, subtotal: 20 });
    expect(screen.getByRole('button', { name: 'Decrease quantity of Clean Code' })).toBeDisabled();
  });

  it('allows removing the item', async () => {
    const { onRemove } = renderRow();
    await userEvent.click(screen.getByRole('button', { name: 'Remove' }));
    expect(onRemove).toHaveBeenCalledTimes(1);
  });
});
