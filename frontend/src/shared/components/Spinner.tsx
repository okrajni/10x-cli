interface SpinnerProps {
  label?: string
  /** Fill the viewport height — used by route-level suspense fallbacks. */
  fullScreen?: boolean
}

/**
 * Loading state built from a single thin accent ring — no color change,
 * no shadow. Text is the same accent as everything else.
 */
export function Spinner({ label = 'Loading', fullScreen = false }: SpinnerProps) {
  return (
    <div
      className={
        fullScreen
          ? 'flex min-h-screen flex-col items-center justify-center gap-4'
          : 'flex flex-col items-center justify-center gap-4 py-16'
      }
      role="status"
      aria-live="polite"
    >
      <span
        aria-hidden="true"
        className="h-8 w-8 animate-spin rounded-pill border border-accent/25 border-t-accent"
      />
      <p className="text-xs font-light uppercase tracking-label text-accent/70">{label}</p>
    </div>
  )
}
