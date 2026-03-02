'use client';

import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { jwtDecode } from 'jwt-decode';
import api from '@/lib/api';
import type { JwtPayload, LoginRequest, Role, TokenResponse } from '@/types/auth';

interface AuthState {
  userId: string | null;
  email: string | null;
  role: Role | null;
  mustChangePassword: boolean;
  isAuthenticated: boolean;
  isLoading: boolean;
}

interface AuthContextValue extends AuthState {
  login: (data: LoginRequest) => Promise<{ role: Role; mustChangePassword: boolean }>;
  logout: () => Promise<void>;
  clearAuth: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function decodeToken(token: string): JwtPayload | null {
  try {
    return jwtDecode<JwtPayload>(token);
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({
    userId: null,
    email: null,
    role: null,
    mustChangePassword: false,
    isAuthenticated: false,
    isLoading: true,
  });

  // Hydrate auth state from localStorage on mount
  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      const payload = decodeToken(token);
      if (payload && payload.exp * 1000 > Date.now()) {
        // Keep cookie in sync so Next.js middleware can read it
        document.cookie = `accessToken=${token}; path=/; SameSite=Lax; max-age=${payload.exp - Math.floor(Date.now() / 1000)}`;
        setState({
          userId: payload.sub,
          email: payload.email,
          role: payload.role,
          mustChangePassword: payload.mustChangePassword ?? false,
          isAuthenticated: true,
          isLoading: false,
        });
        return;
      }
      // Token expired — clear storage and cookie
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      document.cookie = 'accessToken=; path=/; max-age=0';
    }
    setState((prev) => ({ ...prev, isLoading: false }));
  }, []);

  const login = useCallback(async (data: LoginRequest) => {
    const response = await api.post<TokenResponse>('/api/v1/auth/login', data);
    const { accessToken, refreshToken, mustChangePassword } = response.data;

    localStorage.setItem('accessToken', accessToken);
    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken);
    }

    const payload = decodeToken(accessToken);

    // Mirror to cookie so Next.js middleware can read it server-side
    if (payload) {
      document.cookie = `accessToken=${accessToken}; path=/; SameSite=Lax; max-age=${payload.exp - Math.floor(Date.now() / 1000)}`;
    }
    if (!payload) throw new Error('Invalid token received');

    setState({
      userId: payload.sub,
      email: payload.email,
      role: payload.role,
      mustChangePassword: payload.mustChangePassword ?? false,
      isAuthenticated: true,
      isLoading: false,
    });

    return { role: payload.role, mustChangePassword: mustChangePassword ?? false };
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      try {
        await api.post('/api/v1/auth/logout', { refreshToken });
      } catch {
        // Ignore logout errors — clear locally regardless
      }
    }
    clearAuth();
  }, []);

  const clearAuth = useCallback(() => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    document.cookie = 'accessToken=; path=/; max-age=0';
    setState({
      userId: null,
      email: null,
      role: null,
      mustChangePassword: false,
      isAuthenticated: false,
      isLoading: false,
    });
  }, []);

  return (
    <AuthContext.Provider value={{ ...state, login, logout, clearAuth }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
