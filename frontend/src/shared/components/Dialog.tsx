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
        <DialogPrimitive.Overlay className="fixed inset-0 bg-black bg-opacity-50 z-40" />

        <DialogPrimitive.Content className="fixed left-1/2 top-1/2 z-50 w-full max-w-md -translate-x-1/2 -translate-y-1/2 rounded-lg bg-cream-100 p-6 shadow-lg focus:outline-none">
          <div className="flex justify-between items-center mb-4">
            <DialogPrimitive.Title className="text-lg font-semibold text-charcoal">
              {title}
            </DialogPrimitive.Title>
            <DialogPrimitive.Close asChild>
              <button
                className="text-charcoal hover:text-green-600 focus:outline-none focus:ring-2 focus:ring-green-600 rounded"
                aria-label="Close dialog"
              >
                ✕
              </button>
            </DialogPrimitive.Close>
          </div>

          <DialogPrimitive.Description className="text-gray-600">{children}</DialogPrimitive.Description>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  )
}
