export interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string;
  price: number;
  stock: number;
  description?: string;
}

export interface CartItem {
  id: number;
  bookId: number;
  title: string;
  author: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface Cart {
  id?: number;
  items: CartItem[];
  itemCount: number;
  total: number;
}

export interface User {
  id: number;
  email: string;
  displayName: string;
}

export interface AuthResponse {
  user: User;
  accessToken: string;
  expiresIn: number;
}

export interface OrderItem {
  bookId: number;
  title: string;
  author: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  id: number;
  orderNumber: string;
  status: string;
  total: number;
  createdAt: string;
  items: OrderItem[];
}

export interface ApiErrorBody {
  status: number;
  code: string;
  message: string;
  fieldErrors?: Record<string, string>;
}
