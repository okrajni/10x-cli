import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Input } from '@shared/components'
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

    if (hasRecurrence && formData.recurrenceFrequency === 'WEEKLY' && formData.recurrenceWeekday === undefined) {
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
        setMessage({ type: 'success', text: 'Task created successfully!' })
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

  const isFormValid =
    formData.title.trim() && formData.category && formData.dueDate && !isLoading

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-2xl mx-auto">
        <div className="bg-white rounded-lg shadow-md p-6">
          <h1 className="text-3xl font-bold text-gray-900 mb-6">Create New Task</h1>

          {message && (
            <div
              className={clsx(
                'mb-4 p-4 rounded-lg',
                message.type === 'success'
                  ? 'bg-green-50 text-green-800 border border-green-200'
                  : 'bg-red-50 text-red-800 border border-red-200'
              )}
            >
              {message.text}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Title */}
            <div>
              <label htmlFor="title" className="block text-sm font-medium text-gray-700 mb-2">
                Task Title *
              </label>
              <Input
                id="title"
                type="text"
                value={formData.title}
                onChange={(e) => {
                  setFormData({ ...formData, title: e.target.value })
                  if (errors.title) setErrors({ ...errors, title: undefined })
                }}
                placeholder="e.g., Buy groceries, Clean kitchen"
                disabled={isLoading}
                className={errors.title ? 'border-red-500' : ''}
              />
              {errors.title && <p className="mt-1 text-sm text-red-600">{errors.title}</p>}
            </div>

            {/* Description */}
            <div>
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-2">
                Description (optional)
              </label>
              <textarea
                id="description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="Add details about this task..."
                disabled={isLoading}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {/* Category */}
            <div>
              <label htmlFor="category" className="block text-sm font-medium text-gray-700 mb-2">
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
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {categoryLabel(cat)}
                  </option>
                ))}
              </select>
              {errors.category && <p className="mt-1 text-sm text-red-600">{errors.category}</p>}
            </div>

            {/* Due Date */}
            <div>
              <label htmlFor="dueDate" className="block text-sm font-medium text-gray-700 mb-2">
                Due Date *
              </label>
              <Input
                id="dueDate"
                type="date"
                value={formData.dueDate}
                onChange={(e) => {
                  setFormData({ ...formData, dueDate: e.target.value })
                  if (errors.dueDate) setErrors({ ...errors, dueDate: undefined })
                }}
                disabled={isLoading}
                className={errors.dueDate ? 'border-red-500' : ''}
              />
              {errors.dueDate && <p className="mt-1 text-sm text-red-600">{errors.dueDate}</p>}
            </div>

            {/* Recurrence Toggle */}
            <div>
              <label className="flex items-center gap-2">
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
                  className="w-4 h-4 rounded border-gray-300"
                />
                <span className="text-sm font-medium text-gray-700">Repeat this task?</span>
              </label>
            </div>

            {/* Recurrence Frequency */}
            {hasRecurrence && (
              <>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-3">
                    Frequency *
                  </label>
                  <div className="space-y-2">
                    {FREQUENCY_OPTIONS.map((option) => (
                      <label key={option.value} className="flex items-center gap-2">
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
                          className="w-4 h-4 border-gray-300"
                        />
                        <span className="text-sm text-gray-700">{option.label}</span>
                      </label>
                    ))}
                  </div>
                </div>

                {/* Weekday Selector */}
                {formData.recurrenceFrequency === 'WEEKLY' && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-3">
                      Day of Week *
                    </label>
                    <div className="grid grid-cols-4 gap-2">
                      {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((day, index) => (
                        <button
                          key={index}
                          type="button"
                          onClick={() => setFormData({ ...formData, recurrenceWeekday: index })}
                          disabled={isLoading}
                          className={clsx(
                            'py-2 px-3 rounded-md text-sm font-medium transition-colors',
                            formData.recurrenceWeekday === index
                              ? 'bg-blue-500 text-white'
                              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                          )}
                        >
                          {day}
                        </button>
                      ))}
                    </div>
                    {errors.recurrenceWeekday && (
                      <p className="mt-1 text-sm text-red-600">{errors.recurrenceWeekday}</p>
                    )}
                  </div>
                )}

                {/* Recurrence End Date */}
                <div>
                  <label htmlFor="recurrenceEndDate" className="block text-sm font-medium text-gray-700 mb-2">
                    End Date (optional)
                  </label>
                  <Input
                    id="recurrenceEndDate"
                    type="date"
                    value={formData.recurrenceEndDate || ''}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        recurrenceEndDate: e.target.value || undefined,
                      })
                    }
                    disabled={isLoading}
                  />
                  <p className="mt-1 text-xs text-gray-500">Leave empty for no end date</p>
                </div>
              </>
            )}

            {/* Submit Button */}
            <div className="flex gap-3 pt-4">
              <Button
                type="submit"
                disabled={!isFormValid}
                className="flex-1"
              >
                {isLoading ? 'Creating...' : 'Create Task'}
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
    </div>
  )
}

function clsx(...args: (string | undefined | false)[]) {
  return args.filter(Boolean).join(' ')
}
