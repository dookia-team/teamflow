import { create } from 'zustand'

type ToastType = 'success' | 'error' | 'warning' | 'info'

interface ToastItem {
  id: number
  message: string
  type: ToastType
}

interface ToastStore {
  toasts: ToastItem[]
  add: (message: string, type: ToastType) => void
  remove: (id: number) => void
  success: (message: string) => void
  error: (message: string) => void
  warning: (message: string) => void
  info: (message: string) => void
}

const TOAST_AUTO_DISMISS_MS = 3000

let nextId = 0

export const useToastStore = create<ToastStore>((set) => ({
  toasts: [],

  add: (message, type) => {
    const id = nextId++
    set((state) => ({ toasts: [...state.toasts, { id, message, type }] }))
    setTimeout(() => {
      set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) }))
    }, TOAST_AUTO_DISMISS_MS)
  },

  remove: (id) => {
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) }))
  },

  success: (message) => useToastStore.getState().add(message, 'success'),
  error: (message) => useToastStore.getState().add(message, 'error'),
  warning: (message) => useToastStore.getState().add(message, 'warning'),
  info: (message) => useToastStore.getState().add(message, 'info'),
}))

export const toast = {
  success: (message: string) => useToastStore.getState().success(message),
  error: (message: string) => useToastStore.getState().error(message),
  warning: (message: string) => useToastStore.getState().warning(message),
  info: (message: string) => useToastStore.getState().info(message),
}
