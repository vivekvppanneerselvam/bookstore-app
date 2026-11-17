import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { api } from '../api/client';
import { tokenStore } from '../api/tokenStore';
import type { AuthResponse, User } from '../api/types';

type AuthStatus = 'loading' | 'authenticated' | 'anonymous';

interface AuthContextValue {
  user: User | null;
  status: AuthStatus;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

let restorePromise: Promise<AuthResponse | null> | null = null;

function restoreSessionRequest(): Promise<AuthResponse | null> {
  if (!restorePromise) {
    restorePromise = axios
      .post<AuthResponse>('/api/auth/refresh', null, { withCredentials: true })
      .then((response) => response.data)
      .catch(() => null)
      .finally(() => {
        restorePromise = null;
      });
  }
  return restorePromise;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [initializing, setInitializing] = useState(true);
  const queryClient = useQueryClient();

  useEffect(() => {
    let active = true;
    restoreSessionRequest().then((session) => {
      if (!active) {
        return;
      }
      if (session) {
        tokenStore.set(session.accessToken);
        setUser(session.user);
      }
      setInitializing(false);
    });
    return () => {
      active = false;
    };
  }, []);

  useEffect(
    () =>
      tokenStore.subscribe((token) => {
        if (!token) {
          setUser(null);
        }
      }),
    [],
  );

  const login = useCallback(async (email: string, password: string) => {
    const { data } = await api.post<AuthResponse>('/auth/login', { email, password });
    tokenStore.set(data.accessToken);
    setUser(data.user);
  }, []);

  const register = useCallback(async (email: string, password: string, displayName: string) => {
    const { data } = await api.post<AuthResponse>('/auth/register', { email, password, displayName });
    tokenStore.set(data.accessToken);
    setUser(data.user);
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout');
    } catch {
      // Local session is cleared even if the server call fails.
    }
    tokenStore.set(null);
    setUser(null);
    queryClient.clear();
  }, [queryClient]);

  const status: AuthStatus = initializing ? 'loading' : user ? 'authenticated' : 'anonymous';
  const value = useMemo(
    () => ({ user, status, login, register, logout }),
    [user, status, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}
