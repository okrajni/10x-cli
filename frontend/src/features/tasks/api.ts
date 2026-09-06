import { apiClient, ApiResult } from '@lib/api/client'

export interface Task {
  id: string
  title: string
  description?: string
  category: 'cleaning' | 'shopping' | 'laundry' | 'maintenance' | 'bills'
  dueDate?: string
  assignedTo: string
  status: 'pending' | 'completed'
  householdId: string
  createdAt: string
  updatedAt: string
}

export interface CreateTaskRequest {
  title: string
  description?: string
  category: Task['category']
  dueDate?: string
  assignedTo: string
}

export async function createTaskApi(request: CreateTaskRequest): Promise<ApiResult<Task>> {
  return apiClient('/tasks', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function listTasksApi(): Promise<ApiResult<Task[]>> {
  return apiClient('/tasks')
}

export async function getTaskApi(taskId: string): Promise<ApiResult<Task>> {
  return apiClient(`/tasks/${taskId}`)
}

export interface UpdateTaskRequest {
  title?: string
  description?: string
  category?: Task['category']
  dueDate?: string
  assignedTo?: string
  status?: Task['status']
}

export async function updateTaskApi(
  taskId: string,
  request: UpdateTaskRequest
): Promise<ApiResult<Task>> {
  return apiClient(`/tasks/${taskId}`, {
    method: 'PATCH',
    body: JSON.stringify(request),
  })
}

export async function deleteTaskApi(taskId: string): Promise<ApiResult<void>> {
  return apiClient(`/tasks/${taskId}`, {
    method: 'DELETE',
  })
}

export async function completeTaskApi(taskId: string): Promise<ApiResult<Task>> {
  return apiClient(`/tasks/${taskId}`, {
    method: 'PATCH',
    body: JSON.stringify({ status: 'completed' }),
  })
}
