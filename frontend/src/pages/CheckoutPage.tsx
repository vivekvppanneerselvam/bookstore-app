import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../api/client';
import { useCart } from '../api/queries';
import type { Cart, Order } from '../api/types';
import { formatCurrency } from '../utils/format';
import { newIdempotencyKey } from '../utils/idempotency';

const EMPTY_CART: Cart = { items: [], itemCount: 0, total: 0 };

export function CheckoutPage() {
  const { data: cart, isLoading, isError, error } = useCart();
  const [order, setOrder] = useState<Order | null>(null);
  const queryClient = useQueryClient();

  const placeOrder = useMutation<Order, unknown, void>({
    mutationFn: async () => {
      const response = await api.post<Order>('/orders', null, {
        headers: { 'Idempotency-Key': newIdempotencyKey() },
      });
      return response.data;
    },
    onSuccess: (placed) => {
      setOrder(placed);
      queryClient.setQueryData(['cart'], EMPTY_CART);
      queryClient.invalidateQueries({ queryKey: ['books'] });
    },
  });

  if (order) {
    return (
      <section className="narrow">
        <div className="card confirmation">
          <h1>Order confirmed</h1>
          <p className="muted">Thank you for your purchase.</p>
          <dl className="summary-list">
            <div>
              <dt>Order number</dt>
              <dd>{order.orderNumber}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>{order.status}</dd>
            </div>
            <div>
              <dt>Total</dt>
              <dd>{formatCurrency(order.total)}</dd>
            </div>
          </dl>
          <ul className="plain-list">
            {order.items.map((item) => (
              <li key={item.bookId}>
                <span>
                  {item.title} × {item.quantity}
                </span>
                <span>{formatCurrency(item.lineTotal)}</span>
              </li>
            ))}
          </ul>
          <Link to="/" className="btn primary">
            Continue shopping
          </Link>
        </div>
      </section>
    );
  }

  return (
    <section className="narrow">
      <div className="page-heading">
        <h1>Checkout</h1>
      </div>

      {isLoading && <div className="page-loading">Loading order summary…</div>}
      {isError && <div className="alert error">{getErrorMessage(error)}</div>}
      {placeOrder.isError && <div className="alert error">{getErrorMessage(placeOrder.error)}</div>}

      {cart && cart.items.length === 0 && (
        <div className="empty-state">
          <p>There is nothing to check out yet.</p>
          <Link to="/" className="btn primary">
            Browse books
          </Link>
        </div>
      )}

      {cart && cart.items.length > 0 && (
        <div className="card confirmation">
          <h2>Order summary</h2>
          <ul className="plain-list">
            {cart.items.map((item) => (
              <li key={item.id}>
                <span>
                  {item.title} × {item.quantity}
                </span>
                <span>{formatCurrency(item.subtotal)}</span>
              </li>
            ))}
          </ul>
          <div className="total-line">
            <span>Total</span>
            <strong>{formatCurrency(cart.total)}</strong>
          </div>
          <button
            type="button"
            className="btn primary full"
            onClick={() => placeOrder.mutate()}
            disabled={placeOrder.isPending}
          >
            {placeOrder.isPending ? 'Placing order…' : 'Place order'}
          </button>
          <Link to="/cart" className="link-back">
            Back to cart
          </Link>
        </div>
      )}
    </section>
  );
}
