import { twMerge } from 'tailwind-merge'

const sizes = {
  sm: 'w-8 h-8 text-xs',
  md: 'w-10 h-10 text-sm',
  lg: 'w-12 h-12 text-base',
} as const

interface AvatarProps {
  src?: string | null
  name: string
  size?: keyof typeof sizes
  className?: string
}

function getInitials(name: string): string {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

export function Avatar({ src, name, size = 'md', className }: AvatarProps) {
  if (src) {
    return (
      <img
        src={src}
        alt={name}
        className={twMerge('rounded-full object-cover', sizes[size], className)}
      />
    )
  }

  return (
    <div
      className={twMerge(
        'rounded-full bg-primary-100 text-primary-700 font-semibold flex items-center justify-center',
        sizes[size],
        className,
      )}
      aria-label={name}
    >
      {getInitials(name)}
    </div>
  )
}
