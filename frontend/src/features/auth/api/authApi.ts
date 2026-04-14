import axios from 'axios'
import { apiClient } from '@/shared/api/apiClient'
import { env } from '@/shared/config/env'
import type { User } from '@/entities/user'

interface AuthResponse {
  accessToken: string
  user: User
  isNewUser: boolean
}

interface RefreshResponse {
  accessToken: string
}

export const authApi = {
  loginWithGoogle: (code: string, redirectUri: string) =>
    apiClient.post<AuthResponse>('/api/auth/google', { code, redirectUri }),

  refresh: () =>
    axios.post<RefreshResponse>(`${env.apiBaseUrl}/api/auth/refresh`, null, {
      withCredentials: true,
    }),

  logout: () =>
    apiClient.post('/api/auth/logout'),
}
