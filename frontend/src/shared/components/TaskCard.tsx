import clsx from 'clsx'
import { Task } from '@features/tasks/api'
import { categoryColor, categoryLabel } from '@features/tasks/utils/categoryUtils'
import { formatRecurrence } from '@features/tasks/utils/recurrenceUtils'
import { Button } from './Button'

interface TaskCardProps {
  task: Task
  onComplete?: (taskId: string) => void
  onEdit?: (taskId: string) => void
  onDelete?: (taskId: string) => void
  isLoading?: boolean
}

export function TaskCard({
  task,
  onComplete,
  onEdit,
  onDelete,
  isLoading = false,
}: TaskCardProps) {
  const isCompleted = !!task.completedAt
  const dueDate = new Date(task.dueDate)
  const isOverdue = dueDate < new Date() && !isCompleted

  return (
    <div
      className={clsx(
        'p-4 border rounded-md shadow-retro hover:shadow-retro-lg transition bg-cream-100 border-cream-200',
        isCompleted && 'opacity-60',
        isOverdue && 'border-rose-100'
      )}
    >
      {/* Header: Title + Category Badge */}
      <div className="flex justify-between items-start mb-2">
        <h3 className={clsx('font-semibold text-charcoal', isCompleted && 'line-through')}>
          {task.title}
        </h3>
        <span className={clsx('text-xs px-2 py-1 rounded-full font-medium', categoryColor(task.category))}>
          {categoryLabel(task.category)}
        </span>
      </div>

      {/* Description */}
      {task.description && (
        <p className="text-sm text-charcoal mb-3 line-clamp-2">{task.description}</p>
      )}

      {/* Due Date & Recurrence */}
      <div className="flex gap-4 text-sm text-charcoal mb-4 items-center">
        <span className={clsx(isOverdue && 'text-rose-200 font-medium')}>
          {dueDate.toLocaleDateString()}
        </span>
        {task.recurrenceFrequency && (
          <span className="px-2 py-1 bg-cream-200 text-charcoal rounded text-xs font-medium">
            {formatRecurrence(task.recurrenceFrequency, task.recurrenceWeekday, task.recurrenceEndDate)}
          </span>
        )}
      </div>

      {/* Action Buttons */}
      <div className="flex gap-2 justify-end">
        <Button
          variant="primary"
          size="sm"
          onClick={() => onComplete?.(task.id)}
          disabled={isLoading}
          aria-label={`Mark ${task.title} complete`}
        >
          {isCompleted ? '✓ Done' : 'Complete'}
        </Button>

        {onEdit && (
          <Button
            variant="secondary"
            size="sm"
            onClick={() => onEdit(task.id)}
            disabled={isLoading}
            aria-label={`Edit ${task.title}`}
          >
            Edit
          </Button>
        )}

        {onDelete && (
          <Button
            variant="danger"
            size="sm"
            onClick={() => onDelete(task.id)}
            disabled={isLoading}
            aria-label={`Delete ${task.title}`}
          >
            Delete
          </Button>
        )}
      </div>
    </div>
  )
}
