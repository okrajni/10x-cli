import { ReactNode } from 'react'
import clsx from 'clsx'

interface NoticeProps {
  /**
   * Tone never changes the palette — only the weight of the hairline, the
   * fill opacity and the leading glyph. Errors read as errors because they
   * are emphatic, not because they are red.
   */
  tone?: 'info' | 'error' | 'success'
  children: ReactNode
  onDismiss?: () => void
  className?: string
}

const glyphs = {
  info: 'i',
  error: '!',
  success: '✓',
} as const

export function Notice({ tone = 'info', children, onDismiss, className }: NoticeProps) {
  return (
    <div
      role={tone === 'error' ? 'alert' : 'status'}
      className={clsx('notice', tone === 'error' && 'notice-strong', className)}
    >
      <span
        aria-hidden="true"
        className={clsx(
          'mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-pill border text-[0.7rem] font-medium',
          tone === 'error' ? 'border-accent bg-accent text-canvas' : 'border-accent/50 text-accent'
        )}
      >
        {glyphs[tone]}
      </span>

      <div className="flex-1 text-accent">{children}</div>

      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          className="link shrink-0 text-xs"
          aria-label="Dismiss message"
        >
          Dismiss
        </button>
      )}
    </div>
  )
}
