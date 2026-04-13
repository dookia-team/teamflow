import { twMerge } from 'tailwind-merge'

const sizes = {
  sm: 'w-4 h-4 border-2',
  md: 'w-8 h-8 border-4',
  lg: 'w-12 h-12 border-4',
} as const

interface SpinnerProps {
  size?: keyof typeof sizes
  className?: string
}

export function Spinner({ size = 'md', className }: SpinnerProps) {
  return (
    <div
      className={twMerge(
        'border-indigo-500 border-t-transparent rounded-full animate-spin',
        sizes[size],
        className,
      )}
      role="status"
      aria-label="로딩 중"
    />
  )
}
