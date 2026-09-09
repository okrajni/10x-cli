import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import clsx from 'clsx'
import { Button, Input, Notice, PageHeading } from '@shared/components'
import { createTask, CreateTaskRequest, TaskCategory } from '../api'
import { categoryLabel, CATEGORIES } from '../utils/categoryUtils'
import { getTodayDate, FREQUENCY_OPTIONS } from '../utils/recurrenceUtils'
import { useAuth } from '@features/auth/context/AuthContext'

interface FormErrors {
  title?: string
  category?: string
  dueDate?: string
  recurrenceWeekday?: string
}

export default function TaskCreatePage() {
  const navigate = useNavigate()
  useAuth()
  const [formData, setFormData] = useState<CreateTaskRequest>({
    title: '',
    description: '',
    category: 'CLEANING',
    dueDate: getTodayDate(),
  })
  const [hasRecurrence, setHasRecurrence] = useState(false)
  const [errors, setErrors] = useState<FormErrors>({})
  const [isLoading, setIsLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {}

    if (!formData.title.trim()) {
      newErrors.title = 'Title is required'
    }

    if (!formData.category) {
      newErrors.category = 'Category is required'
    }

    if (!formData.dueDate) {
      newErrors.dueDate = 'Due date is required'
    }

    if (
      hasRecurrence &&
      formData.recurrenceFrequency === 'WEEKLY' &&
      formData.recurrenceWeekday === undefined
    ) {
      newErrors.recurrenceWeekday = 'Weekday is required for weekly recurrence'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validateForm()) {
      return
    }

    setIsLoading(true)
    setMessage(null)

    try {
      const submitData = { ...formData }
      if (!hasRecurrence) {
        submitData.recurrenceFrequency = undefined
        submitData.recurrenceWeekday = undefined
        submitData.recurrenceEndDate = undefined
      }

      const result = await createTask(submitData)

      if (result.ok) {
        setMessage({ type: 'success', text: 'Task created successfully.' })
        setFormData({
          title: '',
          description: '',
          category: 'CLEANING',
          dueDate: getTodayDate(),
        })
        setHasRecurrence(false)
        setErrors({})
        setTimeout(() => setMessage(null), 3000)
      } else {
        setMessage({
          type: 'error',
          text: result.message || 'Failed to create task',
        })
      }
    } catch {
      setMessage({
        type: 'error',
        text: 'An error occurred while creating the task',
      })
    } finally {
      setIsLoading(false)
    }
  }

  const isFormValid = formData.title.trim() && formData.category && formData.dueDate && !isLoading

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeading title="Create New Task" subtitle="Add something for the household to do." />

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
            label="Task Title *"
            type="text"
            value={formData.title}
            onChange={(e) => {
              setFormData({ ...formData, title: e.target.value })
              if (errors.title) setErrors({ ...errors, title: undefined })
            }}
            placeholder="e.g., Buy groceries, Clean kitchen"
            disabled={isLoading}
            error={errors.title}
          />

          {/* Description */}
          <div>
            <label htmlFor="description" className="label">
              Description (optional)
            </label>
            <textarea
              id="description"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Add details about this task…"
              disabled={isLoading}
              rows={3}
              className="field-multiline"
            />
          </div>

          {/* Category */}
          <div>
            <label htmlFor="category" className="label">
              Category *
            </label>
            <select
              id="category"
              value={formData.category}
              onChange={(e) => {
                setFormData({ ...formData, category: e.target.value as TaskCategory })
                if (errors.category) setErrors({ ...errors, category: undefined })
              }}
              disabled={isLoading}
              className={clsx('field', errors.category && 'field-invalid')}
            >
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
            label="Due Date *"
            type="date"
            value={formData.dueDate}
            onChange={(e) => {
              setFormData({ ...formData, dueDate: e.target.value })
              if (errors.dueDate) setErrors({ ...errors, dueDate: undefined })
            }}
            disabled={isLoading}
            error={errors.dueDate}
          />

          {/* Recurrence toggle */}
          <label className="flex cursor-pointer items-center gap-3">
            <input
              type="checkbox"
              checked={hasRecurrence}
              onChange={(e) => {
                setHasRecurrence(e.target.checked)
                if (!e.target.checked) {
                  setFormData({
                    ...formData,
                    recurrenceFrequency: undefined,
                    recurrenceWeekday: undefined,
                    recurrenceEndDate: undefined,
                  })
                }
              }}
              disabled={isLoading}
              className="control-check"
            />
            <span className="text-sm text-accent">Repeat this task</span>
          </label>

          {hasRecurrence && (
            <div className="surface-inset space-y-8 p-6">
              {/* Frequency */}
              <div>
                <p className="label">Frequency *</p>
                <div className="space-y-3">
                  {FREQUENCY_OPTIONS.map((option) => (
                    <label
                      key={option.value}
                      className="flex cursor-pointer items-center gap-3 text-sm font-light text-accent"
                    >
                      <input
                        type="radio"
                        name="frequency"
                        value={option.value}
                        checked={formData.recurrenceFrequency === option.value}
                        onChange={(e) =>
                          setFormData({
                            ...formData,
                            recurrenceFrequency: e.target.value,
                            recurrenceWeekday: undefined,
                          })
                        }
                        disabled={isLoading}
                        className="control-radio"
                      />
                      {option.label}
                    </label>
                  ))}
                </div>
              </div>

              {/* Weekday */}
              {formData.recurrenceFrequency === 'WEEKLY' && (
                <div>
                  <p className="label">Day of Week *</p>
                  <div className="flex flex-wrap gap-2">
                    {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((day, index) => (
                      <button
                        key={index}
                        type="button"
                        onClick={() => setFormData({ ...formData, recurrenceWeekday: index })}
                        disabled={isLoading}
                        className={clsx(
                          'rounded-pill border px-4 py-1.5 text-xs transition',
                          formData.recurrenceWeekday === index
                            ? 'border-accent bg-accent font-medium text-canvas'
                            : 'border-accent/40 font-light text-accent hover:bg-accent/10'
                        )}
                      >
                        {day}
                      </button>
                    ))}
                  </div>
                  {errors.recurrenceWeekday && (
                    <p className="mt-2 pl-1 text-xs font-medium text-accent">
                      ! {errors.recurrenceWeekday}
                    </p>
                  )}
                </div>
              )}

              {/* End date */}
              <Input
                id="recurrenceEndDate"
                label="End Date (optional)"
                type="date"
                value={formData.recurrenceEndDate || ''}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    recurrenceEndDate: e.target.value || undefined,
                  })
                }
                disabled={isLoading}
                helpText="Leave empty for no end date."
              />
            </div>
          )}

          {/* Actions */}
          <div className="divider flex flex-wrap gap-3 pt-8">
            <Button type="submit" disabled={!isFormValid} className="flex-1">
              {isLoading ? 'Creating…' : 'Create Task'}
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate('/task')}
              disabled={isLoading}
            >
              Go to List
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
