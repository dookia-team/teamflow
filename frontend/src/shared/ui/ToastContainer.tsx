import { useToastStore } from '../model/useToastStore'

const typeStyles = {
  success: 'bg-success-light text-success-dark border-success/20',
  error: 'bg-error-light text-error-dark border-error/20',
  warning: 'bg-warning-light text-warning-dark border-warning/20',
  info: 'bg-info-light text-info-dark border-info/20',
} as const

export function ToastContainer() {
  const toasts = useToastStore((s) => s.toasts)
  const remove = useToastStore((s) => s.remove)

  if (toasts.length === 0) return null

  return (
    <div className="fixed top-6 left-1/2 -translate-x-1/2 z-[9999] flex flex-col items-center gap-2">
      {toasts.map((t) => (
        <div
          key={t.id}
          className={`flex items-center gap-3 px-4 py-3 rounded-lg border shadow-md animate-slide-in ${typeStyles[t.type]}`}
          role="alert"
        >
          <p className="text-sm font-medium flex-1">{t.message}</p>
          <button
            onClick={() => remove(t.id)}
            className="text-current opacity-50 hover:opacity-100 transition-opacity text-lg leading-none"
          >
            &times;
          </button>
        </div>
      ))}
    </div>
  )
}
