export const env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8082',
  googleClientId: import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '',
  googleRedirectUri: import.meta.env.VITE_GOOGLE_REDIRECT_URI ?? 'http://localhost:5173/auth/callback',
} as const
