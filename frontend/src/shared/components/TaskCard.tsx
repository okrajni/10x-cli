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
        'rounded-card border border-accent/30 bg-accent/[0.05] p-5 transition hover:border-accent/60',
        isCompleted && 'opacity-55',
        // Overdue is signalled by a solid hairline instead of a warning color.
        isOverdue && 'border-accent'
      )}
    >
      {/* Title + category */}
      <div className="mb-3 flex items-start justify-between gap-4">
        <h3
          className={clsx(
            'font-serif text-base font-semibold text-accent',
            isCompleted && 'line-through decoration-accent/60'
          )}
        >
          {task.title}
        </h3>

        <span
          className={clsx(
            'shrink-0 rounded-pill border px-3 py-0.5 text-[0.65rem] font-medium uppercase tracking-label',
            categoryColor(task.category)
          )}
        >
          {categoryLabel(task.category)}
        </span>
      </div>

      {task.description && (
        <p className="mb-4 line-clamp-2 text-sm font-light text-accent/75">{task.description}</p>
      )}

      {/* Due date + recurrence */}
      <div className="mb-5 flex flex-wrap items-center gap-3 text-xs">
        <span className={clsx('font-light text-accent/75', isOverdue && 'font-medium text-accent')}>
          {isOverdue && <span aria-hidden="true">! </span>}
          {dueDate.toLocaleDateString()}
        </span>

        {task.recurrenceFrequency && (
          <span className="chip">
            {formatRecurrence(
              task.recurrenceFrequency,
              task.recurrenceWeekday,
              task.recurrenceEndDate
            )}
          </span>
        )}
      </div>

      {/* Actions */}
      <div className="flex flex-wrap justify-end gap-2">
        <Button
          variant={isCompleted ? 'secondary' : 'primary'}
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
