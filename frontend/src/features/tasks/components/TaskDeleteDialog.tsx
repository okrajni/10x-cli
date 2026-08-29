import { Dialog, Button } from '@shared/components'
import { useState } from 'react'

interface TaskDeleteDialogProps {
  taskTitle: string
  onConfirm: () => void
  onCancel: () => void
}

export default function TaskDeleteDialog({
  taskTitle,
  onConfirm,
  onCancel,
}: TaskDeleteDialogProps) {
  const [isDeleting, setIsDeleting] = useState(false)

  const handleConfirm = async () => {
    setIsDeleting(true)
    try {
      await onConfirm()
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <Dialog open={true} onOpenChange={(open) => !open && onCancel()} title="Delete Task">
      <div className="space-y-4">
        <p className="text-gray-600">
          Are you sure you want to delete <span className="font-semibold">&quot;{taskTitle}&quot;</span>?
          This action can be recovered later.
        </p>

        <div className="flex gap-3 justify-end pt-4">
          <Button
            variant="secondary"
            onClick={onCancel}
            disabled={isDeleting}
          >
            Cancel
          </Button>
          <Button
            variant="danger"
            onClick={handleConfirm}
            disabled={isDeleting}
          >
            {isDeleting ? 'Deleting...' : 'Delete'}
          </Button>
        </div>
      </div>
    </Dialog>
  )
}
