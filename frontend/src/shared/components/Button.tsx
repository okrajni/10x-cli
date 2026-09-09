import { ButtonHTMLAttributes, ReactNode } from 'react'
import clsx from 'clsx'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /**
   * primary   — filled accent pill, canvas-colored label (the default action)
   * secondary — hairline accent outline, accent label (quiet action)
   * danger    — hairline outline that fills to accent on hover (destructive)
   */
  variant?: 'primary' | 'secondary' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  children: ReactNode
}

export function Button({
  variant = 'primary',
  size = 'md',
  className,
  ...props
}: ButtonProps) {
  const variantStyles = {
    primary: 'bg-accent text-canvas border border-accent hover:bg-accent/85 active:bg-accent/75',
    secondary:
      'bg-transparent text-accent border border-accent/50 hover:border-accent hover:bg-accent/10 active:bg-accent/15',
    danger:
      'bg-transparent text-accent border border-accent/70 hover:bg-accent hover:text-canvas active:bg-accent/85',
  }

  const sizeStyles = {
    sm: 'px-4 py-1.5 text-xs',
    md: 'px-6 py-2.5 text-sm',
    lg: 'px-8 py-3 text-base',
  }

  // Pill geometry, hairline border, no shadow — the button reads as light.
  const baseStyles =
    'inline-flex items-center justify-center gap-2 rounded-pill font-medium tracking-wide transition ' +
    'focus:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 ' +
    'focus-visible:ring-offset-canvas disabled:opacity-40 disabled:cursor-not-allowed'

  return (
    <button
      className={clsx(baseStyles, variantStyles[variant], sizeStyles[size], className)}
      {...props}
    />
  )
}
