import { apiClient, ApiResult } from '@lib/api/client'

export type TaskCategory = 'CLEANING' | 'SHOPPING' | 'LAUNDRY' | 'MAINTENANCE' | 'BILLS'

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
}

export interface CreateTaskRequest {
  title: string
  description?: string
  category: TaskCategory
  dueDate: string
}

export interface UpdateTaskRequest {
  title?: string
  description?: string
  category?: TaskCategory
  dueDate?: string
  completedAt?: string
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
