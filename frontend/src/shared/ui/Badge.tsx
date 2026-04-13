import { type HTMLAttributes } from 'react'
import { twMerge } from 'tailwind-merge'

const variants = {
  default: 'bg-gray-100 text-gray-700',
  primary: 'bg-indigo-100 text-indigo-700',
  success: 'bg-emerald-100 text-emerald-700',
  warning: 'bg-amber-100 text-amber-700',
  danger: 'bg-red-100 text-red-700',
} as const

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: keyof typeof variants
}

export function Badge({ variant = 'default', className, children, ...props }: BadgeProps) {
  return (
    <span
      className={twMerge(
        'inline-flex items-center px-2 py-0.5 text-xs font-medium rounded-md',
        variants[variant],
        className,
      )}
      {...props}
    >
      {children}
    </span>
  )
}
