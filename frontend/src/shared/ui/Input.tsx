import { forwardRef, type InputHTMLAttributes } from 'react'
import { twMerge } from 'tailwind-merge'

/**
 * Atom/Input
 *
 * Figma 스펙:
 *   Height: 44px (h-11)
 *   Padding: 14px 좌우
 *   Radius: 10px (rounded-input)
 *   Border: Default(gray-200), Focus(primary-500 2px), Error(error)
 *   Disabled: bg-gray-50, text-gray-400
 *   Placeholder: text-gray-400
 *   Font: 14px Regular
 */

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, className, id, ...props }, ref) => {
    const inputId = id || label?.toLowerCase().replace(/\s+/g, '-')

    return (
      <div className="w-full">
        {label && (
          <label
            htmlFor={inputId}
            className="block mb-1.5 text-sm font-medium text-gray-700"
          >
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          className={twMerge(
            'w-full h-11 px-3.5 text-sm rounded-input border bg-white transition-colors',
            'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500',
            'disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-50 disabled:text-gray-400',
            'placeholder:text-gray-400',
            error
              ? 'border-error focus:ring-error focus:border-error'
              : 'border-gray-200',
            className,
          )}
          {...props}
        />
        {error && <p className="mt-1.5 text-xs text-error">{error}</p>}
      </div>
    )
  },
)

Input.displayName = 'Input'
