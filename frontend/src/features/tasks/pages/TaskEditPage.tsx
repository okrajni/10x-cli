import { useEffect, useState, useCallback } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import clsx from 'clsx'
import { Button, Input, Notice, PageHeading, Spinner } from '@shared/components'
import { getTask, updateTask, Task, UpdateTaskRequest, TaskCategory } from '../api'
import { categoryLabel, CATEGORIES } from '../utils/categoryUtils'
import { formatRecurrence } from '../utils/recurrenceUtils'
import { useAuth } from '@features/auth/context/AuthContext'

interface FormErrors {
  title?: string
  category?: string
  dueDate?: string
}

export default function TaskEditPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  useAuth()
  const [task, setTask] = useState<Task | null>(null)
  const [formData, setFormData] = useState<UpdateTaskRequest>({})
  const [errors, setErrors] = useState<FormErrors>({})
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)

  const fetchTask = useCallback(async () => {
    if (!id) return

    setIsLoading(true)
    try {
      const result = await getTask(id)

      if (result.ok) {
        setTask(result.data)
        setFormData({
          title: result.data.title,
          description: result.data.description,
          category: result.data.category,
          dueDate: result.data.dueDate,
        })
      } else {
        setMessage({
          type: 'error',
          text: result.message || 'Failed to load task',
        })
      }
    } catch {
      setMessage({
        type: 'error',
        text: 'An error occurred while loading the task',
      })
    } finally {
      setIsLoading(false)
    }
  }, [id])

  useEffect(() => {
    fetchTask()
  }, [fetchTask])

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {}

    if (formData.title !== undefined && !formData.title.trim()) {
      newErrors.title = 'Title cannot be empty'
    }

    if (formData.category !== undefined && !formData.category) {
      newErrors.category = 'Category is required'
    }

    if (formData.dueDate !== undefined && !formData.dueDate) {
      newErrors.dueDate = 'Due date is required'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validateForm() || !id) {
      return
    }

    setIsSubmitting(true)
    setMessage(null)

    try {
      const result = await updateTask(id, formData)

      if (result.ok) {
        setMessage({ type: 'success', text: 'Task updated successfully.' })
        setTimeout(() => navigate('/task'), 1500)
      } else {
        setMessage({
          type: 'error',
          text: result.message || 'Failed to update task',
        })
      }
    } catch {
      setMessage({
        type: 'error',
        text: 'An error occurred while updating the task',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return <Spinner label="Loading task" />
  }

  if (!task) {
    return (
      <div className="mx-auto max-w-2xl">
        <div className="surface space-y-5 py-16 text-center">
          <p className="text-sm font-light text-accent/75">Task not found.</p>
          <Button onClick={() => navigate('/task')}>Go to Tasks</Button>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeading title="Edit Task" subtitle={task.title} />

      <div className="surface p-8 sm:p-10">
        {message && (
          <Notice tone={message.type} className="mb-8">
            {message.text}
          </Notice>
        )}

        <form onSubmit={handleSubmit} className="space-y-8">
          {/* Title */}
          <Input
            id="title"
            label="Task Title"
            type="text"
            value={formData.title || ''}
            onChange={(e) => {
              setFormData({ ...formData, title: e.target.value })
              if (errors.title) setErrors({ ...errors, title: undefined })
            }}
            disabled={isSubmitting}
            error={errors.title}
          />

          {/* Description */}
          <div>
            <label htmlFor="description" className="label">
              Description
            </label>
            <textarea
              id="description"
              value={formData.description || ''}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              disabled={isSubmitting}
              rows={3}
              className="field-multiline"
            />
          </div>

          {/* Category */}
          <div>
            <label htmlFor="category" className="label">
              Category
            </label>
            <select
              id="category"
              value={formData.category || ''}
              onChange={(e) => {
                setFormData({ ...formData, category: e.target.value as TaskCategory })
                if (errors.category) setErrors({ ...errors, category: undefined })
              }}
              disabled={isSubmitting}
              className={clsx('field', errors.category && 'field-invalid')}
            >
              <option value="">Select a category</option>
              {CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>
                  {categoryLabel(cat)}
                </option>
              ))}
            </select>
            {errors.category && (
              <p className="mt-2 pl-1 text-xs font-medium text-accent">! {errors.category}</p>
            )}
          </div>

          {/* Due Date */}
          <Input
            id="dueDate"
            label="Due Date"
            type="date"
            value={formData.dueDate || ''}
            onChange={(e) => {
              setFormData({ ...formData, dueDate: e.target.value })
              if (errors.dueDate) setErrors({ ...errors, dueDate: undefined })
            }}
            disabled={isSubmitting}
            error={errors.dueDate}
          />

          {/* Recurrence (read-only) */}
          {task?.recurrenceFrequency && (
            <div className="surface-inset p-6">
              <p className="label">Recurrence</p>
              <p className="text-sm text-accent">
                {formatRecurrence(
                  task.recurrenceFrequency,
                  task.recurrenceWeekday,
                  task.recurrenceEndDate
                )}
              </p>
              <p className="hint mt-3">Recurrence is read-only in this version.</p>
            </div>
          )}

          {/* Actions */}
          <div className="divider flex flex-wrap gap-3 pt-8">
            <Button type="submit" disabled={isSubmitting} className="flex-1">
              {isSubmitting ? 'Saving…' : 'Save Changes'}
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate('/task')}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
