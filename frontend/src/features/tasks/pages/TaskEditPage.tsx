import { useEffect, useState, useCallback } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Input } from '@shared/components'
import { getTask, updateTask, Task, UpdateTaskRequest, TaskCategory } from '../api'
import { categoryLabel, CATEGORIES } from '../utils/categoryUtils'
import { useAuth } from '@features/auth/context/AuthContext'

interface FormErrors {
  title?: string
  category?: string
  dueDate?: string
}

export default function TaskEditPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const { currentHousehold } = useAuth()
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
  }, [id, currentHousehold])

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
        setMessage({ type: 'success', text: 'Task updated successfully!' })
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
    return (
      <div className="min-h-screen bg-gray-50 py-8 px-4">
        <div className="max-w-2xl mx-auto">
          <p className="text-center text-gray-500">Loading task...</p>
        </div>
      </div>
    )
  }

  if (!task) {
    return (
      <div className="min-h-screen bg-gray-50 py-8 px-4">
        <div className="max-w-2xl mx-auto">
          <p className="text-center text-gray-500">Task not found</p>
          <div className="text-center mt-4">
            <Button onClick={() => navigate('/task')}>Go to Tasks</Button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-2xl mx-auto">
        <div className="bg-white rounded-lg shadow-md p-6">
          <h1 className="text-3xl font-bold text-gray-900 mb-6">Edit Task</h1>

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
                Task Title
              </label>
              <Input
                id="title"
                type="text"
                value={formData.title || ''}
                onChange={(e) => {
                  setFormData({ ...formData, title: e.target.value })
                  if (errors.title) setErrors({ ...errors, title: undefined })
                }}
                disabled={isSubmitting}
                className={errors.title ? 'border-red-500' : ''}
              />
              {errors.title && <p className="mt-1 text-sm text-red-600">{errors.title}</p>}
            </div>

            {/* Description */}
            <div>
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-2">
                Description
              </label>
              <textarea
                id="description"
                value={formData.description || ''}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                disabled={isSubmitting}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {/* Category */}
            <div>
              <label htmlFor="category" className="block text-sm font-medium text-gray-700 mb-2">
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
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">Select a category</option>
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
                Due Date
              </label>
              <Input
                id="dueDate"
                type="date"
                value={formData.dueDate || ''}
                onChange={(e) => {
                  setFormData({ ...formData, dueDate: e.target.value })
                  if (errors.dueDate) setErrors({ ...errors, dueDate: undefined })
                }}
                disabled={isSubmitting}
                className={errors.dueDate ? 'border-red-500' : ''}
              />
              {errors.dueDate && <p className="mt-1 text-sm text-red-600">{errors.dueDate}</p>}
            </div>

            {/* Submit Button */}
            <div className="flex gap-3 pt-4">
              <Button
                type="submit"
                disabled={isSubmitting}
                className="flex-1"
              >
                {isSubmitting ? 'Saving...' : 'Save Changes'}
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
    </div>
  )
}

function clsx(...args: (string | undefined | false)[]) {
  return args.filter(Boolean).join(' ')
}
