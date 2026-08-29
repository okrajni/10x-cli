import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Input } from '@shared/components'
import { createTask, CreateTaskRequest, TaskCategory } from '../api'
import { categoryLabel, CATEGORIES } from '../utils/categoryUtils'
import { HouseholdMemberSelect } from '../components/HouseholdMemberSelect'
import { useAuth } from '@features/auth/context/AuthContext'
import { getHouseholdDetailsApi, HouseholdMember } from '@features/household/api'

interface FormErrors {
  title?: string
  category?: string
  dueDate?: string
  assigneeId?: string
}

export default function TaskCreatePage() {
  const navigate = useNavigate()
  const { currentHousehold, user } = useAuth()
  const [formData, setFormData] = useState<CreateTaskRequest>({
    title: '',
    description: '',
    category: 'CLEANING',
    dueDate: '',
    assigneeId: undefined,
  })
  const [householdMembers, setHouseholdMembers] = useState<HouseholdMember[]>([])
  const [membersLoading, setMembersLoading] = useState(true)
  const [errors, setErrors] = useState<FormErrors>({})
  const [isLoading, setIsLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)

  useEffect(() => {
    const fetchMembers = async () => {
      if (!currentHousehold) return
      try {
        const result = await getHouseholdDetailsApi(currentHousehold.householdId)
        if (result.ok && result.data.members) {
          setHouseholdMembers(result.data.members)
          // Set default assignee to current user if available
          if (user && !formData.assigneeId) {
            const currentUserMember = result.data.members.find(
              (m: HouseholdMember) => m.email === user.email
            )
            if (currentUserMember) {
              setFormData((prev) => ({
                ...prev,
                assigneeId: currentUserMember.id,
              }))
            }
          }
        }
      } catch {
        // Silently fail - members optional for MVP
      } finally {
        setMembersLoading(false)
      }
    }

    fetchMembers()
  }, [currentHousehold, user])

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

    if (!formData.assigneeId) {
      newErrors.assigneeId = 'Assignee is required'
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
      const result = await createTask(formData)

      if (result.ok) {
        setMessage({ type: 'success', text: 'Task created successfully!' })
        // Reset form but keep assignee as current user
        const currentUserMember = householdMembers.find(
          (m: HouseholdMember) => m.email === user?.email
        )
        setFormData({
          title: '',
          description: '',
          category: 'CLEANING',
          dueDate: '',
          assigneeId: currentUserMember?.id,
        })
        setErrors({})
        // Keep form on page for next entry instead of navigating
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
    formData.title.trim() && formData.category && formData.dueDate && formData.assigneeId && !isLoading && !membersLoading

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

            {/* Assignee */}
            <div>
              <HouseholdMemberSelect
                members={householdMembers}
                value={formData.assigneeId}
                onChange={(id) => {
                  setFormData({ ...formData, assigneeId: id })
                  if (errors.assigneeId) setErrors({ ...errors, assigneeId: undefined })
                }}
                label="Assign to *"
                required={true}
                disabled={isLoading || membersLoading}
              />
              {errors.assigneeId && <p className="mt-1 text-sm text-red-600">{errors.assigneeId}</p>}
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
