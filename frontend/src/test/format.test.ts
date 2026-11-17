import { describe, expect, it } from 'vitest';
import { formatCurrency } from '../utils/format';

describe('formatCurrency', () => {
  it('formats USD amounts', () => {
    expect(formatCurrency(19.99)).toBe('$19.99');
    expect(formatCurrency(0)).toBe('$0.00');
  });
});
