import { apiClient, ApiResult } from '@lib/api/client'

export type TaskCategory = 'CLEANING' | 'SHOPPING' | 'LAUNDRY' | 'MAINTENANCE' | 'BILLS' | 'ERRANDS' | 'SEASONAL'

export interface Task {
  id: string
  householdId: string
  title: string
  description?: string
  category: TaskCategory
  dueDate: string
  completedAt?: string
  deletedAt?: string
  createdAt: string
  updatedAt: string
  parentTaskId?: string
  recurrenceFrequency?: string
  recurrenceWeekday?: number
  recurrenceEndDate?: string
}

export interface CreateTaskRequest {
  title: string
  description?: string
  category: TaskCategory
  dueDate: string
  recurrenceFrequency?: string
  recurrenceWeekday?: number
  recurrenceEndDate?: string
}

export interface UpdateTaskRequest {
  title?: string
  description?: string
  category?: TaskCategory
  dueDate?: string
  completedAt?: string
  recurrenceFrequency?: string
  recurrenceWeekday?: number
  recurrenceEndDate?: string
}

export async function createTask(request: CreateTaskRequest): Promise<ApiResult<Task>> {
  return apiClient('/task', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function listTasks(): Promise<ApiResult<Task[]>> {
  return apiClient('/task')
}

export async function getTask(taskId: string): Promise<ApiResult<Task>> {
  return apiClient(`/task/${taskId}`)
}

export async function updateTask(
  taskId: string,
  request: UpdateTaskRequest
): Promise<ApiResult<Task>> {
  return apiClient(`/task/${taskId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

export async function deleteTask(taskId: string): Promise<ApiResult<void>> {
  return apiClient(`/task/${taskId}`, {
    method: 'DELETE',
  })
}

export interface DomainTask {
  id: string
  title: string
  category: TaskCategory
  frequencyDays: number
  score: number
}

export interface SuggestionsResponse {
  suggestions: DomainTask[]
}

export async function getSuggestions(): Promise<ApiResult<SuggestionsResponse>> {
  return apiClient('/suggestions/heuristic')
}

export interface SuggestionInteraction {
  suggestionId: string
  action: 'accepted' | 'dismissed'
}

export async function logSuggestionInteraction(
  interaction: SuggestionInteraction
): Promise<ApiResult<void>> {
  return apiClient('/suggestions/interaction', {
    method: 'POST',
    body: JSON.stringify(interaction),
  })
}
