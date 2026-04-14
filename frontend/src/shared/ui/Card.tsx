import { forwardRef, type HTMLAttributes } from 'react'
import { twMerge } from 'tailwind-merge'

/**
 * Organism/Card
 *
 * Figma 스펙:
 *   Padding: sm(16px), md(24px), lg(32px)
 *   Radius: 24px (rounded-card)
 *   Border: gray-200
 *   Shadow: shadow-sm
 */
interface CardProps extends HTMLAttributes<HTMLDivElement> {
  padding?: 'sm' | 'md' | 'lg'
}

const paddings = {
  sm: 'p-4',
  md: 'p-6',
  lg: 'p-8',
} as const

export const Card = forwardRef<HTMLDivElement, CardProps>(
  ({ padding = 'md', className, children, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={twMerge(
          'bg-white rounded-card border border-gray-200 shadow-sm',
          paddings[padding],
          className,
        )}
        {...props}
      >
        {children}
      </div>
    )
  },
)

Card.displayName = 'Card'
