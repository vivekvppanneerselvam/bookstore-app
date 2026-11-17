import type { CartItem } from '../api/types';
import { formatCurrency } from '../utils/format';

interface CartItemRowProps {
  item: CartItem;
  onQuantityChange: (quantity: number) => void;
  onRemove: () => void;
  disabled?: boolean;
}

const MIN_QUANTITY = 1;
const MAX_QUANTITY = 10;

export function CartItemRow({ item, onQuantityChange, onRemove, disabled = false }: CartItemRowProps) {
  return (
    <tr>
      <td>
        <div className="cart-title">{item.title}</div>
        <div className="muted small">{item.author}</div>
      </td>
      <td className="numeric">{formatCurrency(item.unitPrice)}</td>
      <td>
        <div className="quantity-control">
          <button
            type="button"
            aria-label={`Decrease quantity of ${item.title}`}
            onClick={() => onQuantityChange(item.quantity - 1)}
            disabled={disabled || item.quantity <= MIN_QUANTITY}
          >
            −
          </button>
          <span aria-label={`Quantity of ${item.title}`}>{item.quantity}</span>
          <button
            type="button"
            aria-label={`Increase quantity of ${item.title}`}
            onClick={() => onQuantityChange(item.quantity + 1)}
            disabled={disabled || item.quantity >= MAX_QUANTITY}
          >
            +
          </button>
        </div>
      </td>
      <td className="numeric">{formatCurrency(item.subtotal)}</td>
      <td className="numeric">
        <button type="button" className="btn ghost small" onClick={onRemove} disabled={disabled}>
          Remove
        </button>
      </td>
    </tr>
  );
}
