import { forwardRef, type InputHTMLAttributes } from 'react'
import { twMerge } from 'tailwind-merge'

const sizes = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-base',
  lg: 'px-4 py-3 text-lg',
} as const

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  size?: keyof typeof sizes
  error?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ size = 'md', error, className, ...props }, ref) => {
    return (
      <div className="w-full">
        <input
          ref={ref}
          className={twMerge(
            'w-full rounded-lg border bg-white transition-colors',
            'focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500',
            'disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-50',
            'placeholder:text-gray-400',
            error ? 'border-red-500 focus:ring-red-500 focus:border-red-500' : 'border-gray-300',
            sizes[size],
            className,
          )}
          {...props}
        />
        {error && <p className="mt-1 text-sm text-red-500">{error}</p>}
      </div>
    )
  },
)

Input.displayName = 'Input'
