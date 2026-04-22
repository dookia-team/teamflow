import { useToastStore } from '../model/useToastStore'

const typeStyles = {
  success: 'bg-green-50 text-green-800 border-green-200',
  error: 'bg-red-50 text-red-800 border-red-200',
  warning: 'bg-yellow-50 text-yellow-800 border-yellow-200',
  info: 'bg-blue-50 text-blue-800 border-blue-200',
} as const

export function ToastContainer() {
  const toasts = useToastStore((s) => s.toasts)
  const remove = useToastStore((s) => s.remove)

  if (toasts.length === 0) return null

  return (
    <div className="fixed top-6 left-1/2 -translate-x-1/2 z-[100] flex flex-col items-center gap-2">
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
