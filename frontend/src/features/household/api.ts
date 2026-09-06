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
  return apiClient('/api/household', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export interface SendInvitationRequest {
  invitedEmail: string
}

export interface SendInvitationResponse {
  invitationId: string
  invitedEmail: string
  expiresAt: string
}

export async function sendInvitationApi(
  householdId: string,
  request: SendInvitationRequest
): Promise<ApiResult<SendInvitationResponse>> {
  return apiClient(`/api/household/${householdId}/invite`, {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function acceptInvitationApi(
  token: string
): Promise<ApiResult<CreateHouseholdResponse>> {
  return apiClient(`/api/invitation/${token}/accept`, {
    method: 'POST',
  })
}

export async function getUserHouseholdsApi(): Promise<ApiResult<Household[]>> {
  return apiClient('/api/household', {
    method: 'GET',
  })
}
