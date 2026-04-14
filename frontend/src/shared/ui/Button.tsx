import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { twMerge } from 'tailwind-merge'

/**
 * Atom/Button
 *
 * Figma 스펙:
 *   Variant: Primary(#4F46E5), Secondary(white+border), Ghost(transparent), Danger(#EF4444)
 *   Size: sm(h:32, px:12, text:13), md(h:40, px:16, text:14), lg(h:48, px:24, text:16)
 *   Radius: 10px (rounded-button)
 *   Font: Semi Bold
 */

const variants = {
  primary:
    'bg-primary-600 text-white hover:bg-primary-700 active:bg-primary-800 shadow-primary-glow',
  secondary:
    'bg-white text-gray-700 border border-gray-200 hover:bg-gray-50 active:bg-gray-100',
  ghost:
    'bg-transparent text-gray-500 hover:bg-gray-100 active:bg-gray-200',
  danger:
    'bg-error text-white hover:bg-red-600 active:bg-red-700',
} as const

const sizes = {
  sm: 'h-8 px-3 text-[13px]',
  md: 'h-10 px-4 text-sm',
  lg: 'h-12 px-6 text-base',
} as const

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: keyof typeof variants
  size?: keyof typeof sizes
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', size = 'md', className, disabled, children, ...props }, ref) => {
    return (
      <button
        ref={ref}
        disabled={disabled}
        className={twMerge(
          'inline-flex items-center justify-center font-semibold rounded-button transition-colors',
          'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2',
          'disabled:opacity-50 disabled:cursor-not-allowed',
          variants[variant],
          sizes[size],
          className,
        )}
        {...props}
      >
        {children}
      </button>
    )
  },
)

Button.displayName = 'Button'
