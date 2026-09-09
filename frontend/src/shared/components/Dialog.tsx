import * as DialogPrimitive from '@radix-ui/react-dialog'
import { ReactNode } from 'react'

interface DialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  children: ReactNode
}

export function Dialog({ open, onOpenChange, title, children }: DialogProps) {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        {/* The overlay dims with the canvas color itself — no black scrim. */}
        <DialogPrimitive.Overlay className="fixed inset-0 z-40 bg-canvas/80 backdrop-blur-sm" />

        <DialogPrimitive.Content
          className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2
            -translate-y-1/2 rounded-card border border-accent/45 bg-canvas p-8 focus:outline-none"
        >
          <div className="mb-6 flex items-start justify-between gap-6">
            <DialogPrimitive.Title className="font-serif text-xl font-semibold text-accent">
              {title}
            </DialogPrimitive.Title>

            <DialogPrimitive.Close asChild>
              <button
                className="flex h-8 w-8 shrink-0 items-center justify-center rounded-pill border
                  border-accent/40 text-accent transition hover:bg-accent hover:text-canvas
                  focus:outline-none focus-visible:ring-2 focus-visible:ring-accent
                  focus-visible:ring-offset-2 focus-visible:ring-offset-canvas"
                aria-label="Close dialog"
              >
                ✕
              </button>
            </DialogPrimitive.Close>
          </div>

          <DialogPrimitive.Description asChild>
            <div className="text-sm text-accent/85">{children}</div>
          </DialogPrimitive.Description>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  )
}
