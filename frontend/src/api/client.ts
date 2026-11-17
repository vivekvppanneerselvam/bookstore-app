import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { tokenStore } from './tokenStore';
import type { ApiErrorBody, AuthResponse } from './types';

export const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  try {
    const { data } = await axios.post<AuthResponse>('/api/auth/refresh', null, { withCredentials: true });
    tokenStore.set(data.accessToken);
    return data.accessToken;
  } catch {
    tokenStore.set(null);
    return null;
  }
}

function refreshOnce(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined;
    const isAuthEndpoint = original?.url?.startsWith('/auth/') ?? false;
    if (error.response?.status !== 401 || !original || original._retry || isAuthEndpoint) {
      return Promise.reject(error);
    }

    original._retry = true;
    const token = await refreshOnce();
    if (!token) {
      return Promise.reject(error);
    }
    original.headers.Authorization = `Bearer ${token}`;
    return api(original);
  },
);

export function getErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return 'Cannot reach the server. Make sure the backend is running.';
    }
    const body = error.response.data as ApiErrorBody | undefined;
    if (body?.fieldErrors) {
      const first = Object.values(body.fieldErrors)[0];
      if (first) {
        return first;
      }
    }
    if (body?.message) {
      return body.message;
    }
  }
  return fallback;
}
