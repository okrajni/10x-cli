import { apiClient, ApiResult } from '@lib/api/client'

export interface Household {
  id: string
  name: string
  createdAt: string
  members: Array<{ userId: string; email: string; name?: string }>
}

export interface CreateHouseholdRequest {
  name: string
}

export async function createHouseholdApi(
  request: CreateHouseholdRequest
): Promise<ApiResult<Household>> {
  return apiClient('/households', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export interface InvitePartnerRequest {
  email: string
}

export interface InvitePartnerResponse {
  inviteId: string
  inviteLink: string
}

export async function invitePartnerApi(
  householdId: string,
  request: InvitePartnerRequest
): Promise<ApiResult<InvitePartnerResponse>> {
  return apiClient(`/households/${householdId}/invites`, {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export interface JoinHouseholdRequest {
  inviteCode: string
}

export async function joinHouseholdApi(
  request: JoinHouseholdRequest
): Promise<ApiResult<Household>> {
  return apiClient('/households/join', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function getHouseholdApi(householdId: string): Promise<ApiResult<Household>> {
  return apiClient(`/households/${householdId}`)
}
