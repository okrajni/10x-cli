import { apiClient, ApiResult } from '@lib/api/client'

export interface HouseholdMember {
  id: string
  name: string
  email: string
}

export interface Household {
  householdId: string
  name: string
  createdBy: string
  createdAt: string
  members?: HouseholdMember[]
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
  return apiClient(`/household/${householdId}/invite`, {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function acceptInvitationApi(
  token: string
): Promise<ApiResult<CreateHouseholdResponse>> {
  return apiClient(`/invitation/${token}/accept`, {
    method: 'POST',
  })
}

export async function getUserHouseholdsApi(): Promise<ApiResult<Household[]>> {
  return apiClient('/household', {
    method: 'GET',
  })
}
