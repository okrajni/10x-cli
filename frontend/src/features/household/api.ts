import { apiClient, ApiResult } from '@lib/api/client'

export interface Household {
  householdId: string
  name: string
  createdBy: string
  createdAt: string
}

export interface CreateHouseholdRequest {
  name: string
}

export interface CreateHouseholdResponse {
  householdId: string
  name: string
  createdBy: string
  createdAt: string
}

export async function createHouseholdApi(
  request: CreateHouseholdRequest
): Promise<ApiResult<CreateHouseholdResponse>> {
  return apiClient('/household', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function getHouseholdDetailsApi(
  householdId: string
): Promise<ApiResult<Household>> {
  return apiClient(`/household/${householdId}`)
}

export async function getUserHouseholdsApi(): Promise<ApiResult<Household[]>> {
  return apiClient('/household', {
    method: 'GET',
  })
}
