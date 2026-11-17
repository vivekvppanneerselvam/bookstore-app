import { useQuery } from '@tanstack/react-query';
import { api } from './client';
import type { Book, Cart } from './types';

export function useBooks() {
  return useQuery({
    queryKey: ['books'],
    queryFn: async () => (await api.get<Book[]>('/books')).data,
    staleTime: 30_000,
  });
}

export function useCart(enabled = true) {
  return useQuery({
    queryKey: ['cart'],
    queryFn: async () => (await api.get<Cart>('/cart')).data,
    enabled,
    staleTime: 5_000,
  });
}
