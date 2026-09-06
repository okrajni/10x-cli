// Discriminated union for type-safe error handling
export type ApiResult<T> =
  | { ok: true; data: T }
  | { ok: false; code: string; message: string; status: number }

interface ApiClientOptions extends RequestInit {
  headers?: Record<string, string>
}

/**
 * Custom fetch wrapper that:
 * - Injects Bearer token in Authorization header
 * - Discriminates error types (network vs 4xx vs 5xx)
 * - Retries on recoverable errors (5xx, network timeouts)
 * - Returns type-safe ApiResult<T>
 */
export async function apiClient<T>(
  path: string,
  options: ApiClientOptions = {}
): Promise<ApiResult<T>> {
  const token = getStoredToken()

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  }

  const maxRetries = 3
  let lastError: Error | null = null

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      const response = await fetch(`/api${path}`, {
        ...options,
        headers,
        signal: AbortSignal.timeout(30000), // 30s timeout
      })

      // Success: 2xx status
      if (response.ok) {
        const data = await response.json()
        return { ok: true, data }
      }

      // 401 Unauthorized: Token expired, should refresh
      if (response.status === 401) {
        return {
          ok: false,
          code: 'UNAUTHORIZED',
          message: 'Session expired. Please log in again.',
          status: 401,
        }
      }

      // 4xx Client Error: Don't retry
      if (response.status < 500) {
        const errorData = await response.json().catch(() => ({}))
        return {
          ok: false,
          code: errorData.code || `HTTP_${response.status}`,
          message: errorData.message || `Request failed with status ${response.status}`,
          status: response.status,
        }
      }

      // 5xx Server Error: Retry after delay
      await new Promise((resolve) => setTimeout(resolve, Math.pow(2, attempt) * 1000))
      continue
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error))

      // Network error or timeout: Retry
      if (attempt < maxRetries - 1) {
        await new Promise((resolve) => setTimeout(resolve, Math.pow(2, attempt) * 1000))
        continue
      }
    }
  }

  // All retries exhausted
  return {
    ok: false,
    code: 'NETWORK_ERROR',
    message: lastError?.message || 'Network request failed',
    status: 0,
  }
}

function getStoredToken(): string | null {
  try {
    const authState = localStorage.getItem('authState')
    if (authState) {
      const parsed = JSON.parse(authState)
      return parsed.token || null
    }
  } catch {
    // Silently fail
  }
  return null
}
