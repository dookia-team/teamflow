import axios from 'axios'
import { env } from '@/shared/config/env'
import { useAuthStore } from '@/features/auth/model/authStore'

export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true,
})

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

const processQueue = (error: unknown, token: string | null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (token) resolve(token)
    else reject(error)
  })
  failedQueue = []
}

// ApiResponse<T> 자동 언래핑: { success, data, message, timestamp } → data만 추출
apiClient.interceptors.response.use(
  (response) => {
    if (response.data && typeof response.data.success === 'boolean') {
      response.data = response.data.data
    }
    return response
  },
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error)
    }

    // AUTH_TOKEN_REUSED: 토큰 도난 감지 → 즉시 로그아웃
    if (error.response?.data?.error?.code === 'AUTH_TOKEN_REUSED') {
      useAuthStore.getState().clearAuth()
      window.location.href = '/login'
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({
          resolve: (token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(apiClient(originalRequest))
          },
          reject,
        })
      })
    }

    originalRequest._retry = true
    isRefreshing = true

    try {
      const { data } = await axios.post(
        `${env.apiBaseUrl}/api/auth/refresh`,
        null,
        { withCredentials: true },
      )
      const newToken = data.accessToken
      useAuthStore.getState().setAccessToken(newToken)
      processQueue(null, newToken)
      originalRequest.headers.Authorization = `Bearer ${newToken}`
      return apiClient(originalRequest)
    } catch (refreshError) {
      processQueue(refreshError, null)
      useAuthStore.getState().clearAuth()
      window.location.href = '/login'
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  },
)
