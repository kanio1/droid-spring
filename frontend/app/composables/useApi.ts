// API composable - wrapper for fetch with auth, error handling, and toast notifications

interface ApiOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  body?: any
  query?: Record<string, string | number | boolean>
  headers?: Record<string, string>
  skipAuth?: boolean
  skipErrorToast?: boolean
  skipLoading?: boolean
}

interface ApiResponse<T = any> {
  data: T
  ok: boolean
  status: number
  statusText: string
  headers: Headers
}

interface PaginatedResponse<T = any> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
}

export const useApi = () => {
  const config = useRuntimeConfig()
  const { token } = useAuth()
  const toast = useToast()
  const loading = ref(false)

  const baseURL = config.public.apiBaseUrl || 'http://localhost:8080/api'

  // Get auth headers
  const getAuthHeaders = (): Record<string, string> => {
    try {
      return token.value ? { 'Authorization': `Bearer ${token.value}` } : {}
    } catch (error) {
      console.warn('Failed to get auth token:', error)
      return {}
    }
  }

  // Build URL with query parameters
  const buildUrl = (endpoint: string, query?: Record<string, string | number | boolean>): string => {
    const url = new URL(`${baseURL}${endpoint}`)
    
    if (query) {
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          url.searchParams.set(key, String(value))
        }
      })
    }
    
    return url.toString()
  }

  // Handle API errors
  const handleError = (error: any, skipErrorToast = false) => {
    const message = error?.message || error?.error?.message || 'An unexpected error occurred'

    if (!skipErrorToast) {
      toast.add({
        severity: 'error',
        summary: 'Error',
        detail: message,
        life: 5000
      })
    }

    throw error
  }

  // Generic API request method
  const request = async <T = any>(
    endpoint: string,
    options: ApiOptions = {}
  ): Promise<ApiResponse<T>> => {
    const {
      method = 'GET',
      body,
      query,
      headers = {},
      skipAuth = false,
      skipErrorToast = false,
      skipLoading = false
    } = options

    if (!skipLoading) {
      loading.value = true
    }

    try {
      // Build URL
      const url = buildUrl(endpoint, query)
      
      // Prepare headers
      const authHeaders = skipAuth ? {} : getAuthHeaders()
      const requestHeaders = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...authHeaders,
        ...headers
      }

      // Prepare fetch options
      const fetchOptions: RequestInit = {
        method,
        headers: requestHeaders
      }

      if (body && method !== 'GET') {
        fetchOptions.body = typeof body === 'string' ? body : JSON.stringify(body)
      }

      // Make request
      const response = await fetch(url, fetchOptions)
      
      // Handle response
      const responseData = await response.json().catch(() => null)
      
      const apiResponse: ApiResponse<T> = {
        data: responseData,
        ok: response.ok,
        status: response.status,
        statusText: response.statusText,
        headers: response.headers
      }

      // Handle HTTP errors
      if (!response.ok) {
        const error = new Error(apiResponse.data?.message || apiResponse.data?.error || response.statusText)
        ;(error as any).status = response.status
        ;(error as any).data = responseData
        handleError(error, skipErrorToast)
      }

      return apiResponse

    } catch (error) {
      // Network or other errors
      if (!skipErrorToast) {
        handleError(error, skipErrorToast)
      }
      throw error
    } finally {
      if (!skipLoading) {
        loading.value = false
      }
    }
  }

  // Convenience methods for common HTTP verbs
  const get = <T = any>(endpoint: string, options: Omit<ApiOptions, 'method'> = {}) => {
    return request<T>(endpoint, { ...options, method: 'GET' })
  }

  const post = <T = any>(endpoint: string, body?: any, options: Omit<ApiOptions, 'method' | 'body'> = {}) => {
    return request<T>(endpoint, { ...options, method: 'POST', body })
  }

  const put = <T = any>(endpoint: string, body?: any, options: Omit<ApiOptions, 'method' | 'body'> = {}) => {
    return request<T>(endpoint, { ...options, method: 'PUT', body })
  }

  const patch = <T = any>(endpoint: string, body?: any, options: Omit<ApiOptions, 'method' | 'body'> = {}) => {
    return request<T>(endpoint, { ...options, method: 'PATCH', body })
  }

  const del = <T = any>(endpoint: string, options: Omit<ApiOptions, 'method'> = {}) => {
    return request<T>(endpoint, { ...options, method: 'DELETE' })
  }

  // Specific methods for CRUD operations
  const create = <T = any>(endpoint: string, data: any, options: Omit<ApiOptions, 'method' | 'body'> = {}) => {
    return post<T>(endpoint, data, options)
  }

  const read = <T = any>(endpoint: string, query?: Record<string, string | number | boolean>, options: Omit<ApiOptions, 'method' | 'query'> = {}) => {
    return get<T>(endpoint, { ...options, query })
  }

  const update = <T = any>(endpoint: string, data: any, options: Omit<ApiOptions, 'method' | 'body'> = {}) => {
    return put<T>(endpoint, data, options)
  }

  const remove = <T = any>(endpoint: string, options: Omit<ApiOptions, 'method'> = {}) => {
    return del<T>(endpoint, options)
  }

  // Pagination helpers
  const paginatedGet = <T = any>(
    endpoint: string,
    page = 0,
    size = 20,
    sort = 'createdAt,desc',
    query?: Record<string, string | number | boolean>,
    options: Omit<ApiOptions, 'method' | 'query'> = {}
  ) => {
    const paginationQuery = {
      page,
      size,
      sort,
      ...query
    }
    
    return get<PaginatedResponse<T>>(endpoint, { ...options, query: paginationQuery })
  }

  // Success toast helper
  const handleSuccess = (message?: string) => {
    if (message) {
      toast.add({
        severity: 'success',
        summary: 'Success',
        detail: message,
        life: 3000
      })
    }
  }

  return {
    // State
    loading: readonly(loading),
    
    // Core methods
    request,
    get,
    post,
    put,
    patch,
    delete: del,
    
    // CRUD helpers
    create,
    read,
    update,
    remove,
    
    // Pagination
    paginatedGet,
    
    // Utilities
    handleSuccess,
    buildUrl,
    baseURL
  }
}

// Type helpers for API responses
export type ApiData<T> = T
export type ApiListResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
}

export type ApiResponse<T> = {
  data: T
  ok: boolean
  status: number
  statusText: string
  headers: Headers
}
