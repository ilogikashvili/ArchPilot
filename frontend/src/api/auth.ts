import { api, json, storeTokens, clearTokens, type Tokens } from "./client";
export interface User { id: string; email: string; displayName: string; avatarUrl: string | null; emailVerified: boolean; createdAt: string }
export const auth = {
  me: () => api<User>("/auth/me"),
  async login(email: string, password: string) { storeTokens(await api<Tokens>("/auth/login", json("POST", { email: email.trim(), password }), false)); },
  async register(email: string, password: string, displayName: string) { storeTokens(await api<Tokens>("/auth/register", json("POST", { email: email.trim(), password, displayName: displayName.trim() }), false)); },
  async logout() { try { await api<void>("/auth/logout", json("POST")); } finally { clearTokens(); } },
  forgot: (email: string) => api<void>("/auth/forgot-password", json("POST", { email }), false),
  reset: (token: string, newPassword: string) => api<void>("/auth/reset-password", json("POST", { token, newPassword }), false),
  verify: (token: string) => api<void>("/auth/verify-email", json("POST", { token }), false),
  resend: (email: string) => api<void>("/auth/resend-verification", json("POST", { email }), false),
};
