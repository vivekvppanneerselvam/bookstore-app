import { Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../api/client';
import { useCart } from '../api/queries';
import type { Cart } from '../api/types';
import { CartItemRow } from '../components/CartItemRow';
import { formatCurrency } from '../utils/format';

export function CartPage() {
  const { data: cart, isLoading, isError, error } = useCart();
  const queryClient = useQueryClient();

  const onCartChanged = (updated: Cart) => {
    queryClient.setQueryData(['cart'], updated);
    queryClient.invalidateQueries({ queryKey: ['books'] });
  };

  const updateQuantity = useMutation<Cart, unknown, { itemId: number; quantity: number }>({
    mutationFn: async ({ itemId, quantity }) =>
      (await api.patch<Cart>(`/cart/items/${itemId}`, { quantity })).data,
    onSuccess: onCartChanged,
  });

  const removeItem = useMutation<Cart, unknown, number>({
    mutationFn: async (itemId) => (await api.delete<Cart>(`/cart/items/${itemId}`)).data,
    onSuccess: onCartChanged,
  });

  const busy = updateQuantity.isPending || removeItem.isPending;

  return (
    <section>
      <div className="page-heading">
        <h1>Your cart</h1>
      </div>

      {updateQuantity.isError && <div className="alert error">{getErrorMessage(updateQuantity.error)}</div>}
      {removeItem.isError && <div className="alert error">{getErrorMessage(removeItem.error)}</div>}

      {isLoading && <div className="page-loading">Loading cart…</div>}
      {isError && <div className="alert error">{getErrorMessage(error)}</div>}

      {cart && cart.items.length === 0 && (
        <div className="empty-state">
          <p>Your cart is empty.</p>
          <Link to="/" className="btn primary">
            Browse books
          </Link>
        </div>
      )}

      {cart && cart.items.length > 0 && (
        <>
          <div className="card table-wrap">
            <table className="cart-table">
              <thead>
                <tr>
                  <th>Book</th>
                  <th className="numeric">Price</th>
                  <th>Quantity</th>
                  <th className="numeric">Subtotal</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {cart.items.map((item) => (
                  <CartItemRow
                    key={item.id}
                    item={item}
                    disabled={busy}
                    onQuantityChange={(quantity) => updateQuantity.mutate({ itemId: item.id, quantity })}
                    onRemove={() => removeItem.mutate(item.id)}
                  />
                ))}
              </tbody>
            </table>
          </div>

          <div className="cart-summary">
            <div>
              <span className="muted">Items: {cart.itemCount}</span>
              <div className="total-line">
                <span>Total</span>
                <strong>{formatCurrency(cart.total)}</strong>
              </div>
            </div>
            <Link to="/checkout" className="btn primary">
              Proceed to checkout
            </Link>
          </div>
        </>
      )}
    </section>
  );
}
