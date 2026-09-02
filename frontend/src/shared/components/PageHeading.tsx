import { ReactNode } from 'react'

interface PageHeadingProps {
  title: string
  subtitle?: ReactNode
  /** Trailing action, typically a Button. */
  action?: ReactNode
}

/**
 * One consistent page header across every screen: serif title, light
 * accent subtitle, optional action on the right. Keeps vertical rhythm
 * and typographic hierarchy identical app-wide.
 */
export function PageHeading({ title, subtitle, action }: PageHeadingProps) {
  return (
    <div className="mb-10 flex flex-wrap items-end justify-between gap-6">
      <div className="space-y-2">
        <h1 className="text-3xl font-semibold text-accent sm:text-4xl">{title}</h1>
        {subtitle && <p className="max-w-xl text-sm font-light text-accent/70">{subtitle}</p>}
      </div>

      {action && <div className="shrink-0">{action}</div>}
    </div>
  )
}
