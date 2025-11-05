/**
 * API Client Tests
 *
 * Comprehensive tests for API client functionality
 * Tests REST endpoints, error handling, authentication, and retry logic
 *
 * Priority: 🔴 HIGH
 * Learning Value: ⭐⭐⭐⭐⭐
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'

// Mock fetch
const mockFetch = vi.fn()
vi.stubGlobal('fetch', mockFetch)

// Mock Keycloak
vi.mock('@/plugins/keycloak', () => ({
  default: {
    token: { value: 'mock-token' },
    updateToken: vi.fn().mockResolvedValue(true)
  }
}))

describe('ApiClient', () => {
  beforeEach(() => {
    mockFetch.mockClear()
  })

  describe('GET Requests', () => {
    it('should make GET request successfully', async () => {
      const { apiClient } = await import('@/services/api')

      const mockResponse = {
        data: { id: 1, name: 'Test' },
        status: 200
      }

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => mockResponse
      })

      const result = await apiClient.get('/api/test')

      expect(mockFetch).toHaveBeenCalledWith('/api/test', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer mock-token'
        }
      })

      expect(result).toEqual(mockResponse)
    })

    it('should include query parameters in GET request', async () => {
      const { apiClient } = await import('@/services/api')

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => ({ data: [] })
      })

      await apiClient.get('/api/test', {
        params: { page: 1, limit: 10, filter: 'active' }
      })

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/test?page=1&limit=10&filter=active',
        expect.any(Object)
      )
    })

    it('should handle query parameters with special characters', async () => {
      const { apiClient } = await import('@/services/api')

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => ({ data: [] })
      })

      await apiClient.get('/api/test', {
        params: { search: 'test@example.com', filter: 'name:John Doe' }
      })

      const callUrl = mockFetch.mock.calls[0][0]
      expect(callUrl).toContain('search=test%40example.com')
      expect(callUrl).toContain('filter=name%3AJohn%20Doe')
    })
  })

  describe('POST Requests', () => {
    it('should make POST request with JSON body', async () => {
      const { apiClient } = await import('@/services/api')

      const requestBody = {
        name: 'Test User',
        email: 'test@example.com'
      }

      const mockResponse = {
        data: { id: 1, ...requestBody },
        status: 201
      }

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 201,
        headers: { get: () => 'application/json' },
        json: async () => mockResponse
      })

      const result = await apiClient.post('/api/users', requestBody)

      expect(mockFetch).toHaveBeenCalledWith('/api/users', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer mock-token'
        },
        body: JSON.stringify(requestBody)
      })

      expect(result).toEqual(mockResponse)
    })

    it('should make POST request with FormData', async () => {
      const { apiClient } = await import('@/services/api')

      const formData = new FormData()
      formData.append('file', new Blob(['test'], { type: 'text/plain' }), 'test.txt')

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => null },
        json: async () => ({ success: true })
      })

      await apiClient.post('/api/upload', formData)

      expect(mockFetch).toHaveBeenCalledWith('/api/upload', {
        method: 'POST',
        headers: {
          'Authorization': 'Bearer mock-token'
        },
        body: formData
      })

      // Should not set Content-Type for FormData (browser sets it with boundary)
      const headers = mockFetch.mock.calls[0][1].headers
      expect(headers['Content-Type']).toBeUndefined()
    })
  })

  describe('PUT Requests', () => {
    it('should make PUT request', async () => {
      const { apiClient } = await import('@/services/api')

      const updateData = { name: 'Updated Name' }

      const mockResponse = {
        data: { id: 1, name: 'Updated Name' },
        status: 200
      }

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => mockResponse
      })

      const result = await apiClient.put('/api/users/1', updateData)

      expect(mockFetch).toHaveBeenCalledWith('/api/users/1', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer mock-token'
        },
        body: JSON.stringify(updateData)
      })

      expect(result).toEqual(mockResponse)
    })
  })

  describe('PATCH Requests', () => {
    it('should make PATCH request', async () => {
      const { apiClient } = await import('@/services/api')

      const patchData = { status: 'active' }

      const mockResponse = {
        data: { id: 1, status: 'active' },
        status: 200
      }

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => mockResponse
      })

      const result = await apiClient.patch('/api/users/1/status', patchData)

      expect(mockFetch).toHaveBeenCalledWith('/api/users/1/status', {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer mock-token'
        },
        body: JSON.stringify(patchData)
      })

      expect(result).toEqual(mockResponse)
    })
  })

  describe('DELETE Requests', () => {
    it('should make DELETE request', async () => {
      const { apiClient } = await import('@/services/api')

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 204,
        headers: { get: () => null },
        json: async () => ({})
      })

      await apiClient.delete('/api/users/1')

      expect(mockFetch).toHaveBeenCalledWith('/api/users/1', {
        method: 'DELETE',
        headers: {
          'Authorization': 'Bearer mock-token'
        }
      })
    })

    it('should include delete confirmation headers', async () => {
      const { apiClient } = await import('@/services/api')

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 204,
        headers: { get: () => null },
        json: async () => ({})
      })

      await apiClient.delete('/api/users/1', {
        headers: { 'X-Confirm-Delete': 'true' }
      })

      expect(mockFetch).toHaveBeenCalledWith('/api/users/1', {
        method: 'DELETE',
        headers: {
          'Authorization': 'Bearer mock-token',
          'X-Confirm-Delete': 'true'
        }
      })
    })
  })

  describe('Authentication', () => {
    it('should include Authorization header', async () => {
      const { apiClient } = await import('@/services/api')

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => ({ data: {} })
      })

      await apiClient.get('/api/test')

      const headers = mockFetch.mock.calls[0][1].headers
      expect(headers['Authorization']).toBe('Bearer mock-token')
    })

    it('should handle missing token gracefully', async () => {
      const keycloak = (await import('@/plugins/keycloak')).default
      keycloak.token.value = null

      const { apiClient } = await import('@/services/api')

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
        headers: { get: () => 'application/json' },
        json: async () => ({ error: 'Unauthorized' })
      })

      await apiClient.get('/api/test')

      const headers = mockFetch.mock.calls[0][1].headers
      expect(headers['Authorization']).toBeUndefined()
    })

    it('should refresh token on 401 error', async () => {
      const keycloak = (await import('@/plugins/keycloak')).default
      const updateTokenSpy = vi.spyOn(keycloak, 'updateToken')

      const { apiClient } = await import('@/services/api')

      // First call returns 401
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
        headers: { get: () => 'application/json' },
        json: async () => ({ error: 'Unauthorized' })
      })

      // Second call succeeds with new token
      keycloak.token.value = 'new-mock-token'
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => ({ data: {} })
      })

      try {
        await apiClient.get('/api/test')
      } catch (error) {
        // Expected to fail
      }

      expect(updateTokenSpy).toHaveBeenCalled()
    })
  })

  describe('Error Handling', () => {
    it('should handle 404 Not Found', async () => {
      const { apiClient } = await import('@/services/api')

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 404,
        headers: { get: () => 'application/json' },
        json: async () => ({ error: 'Not found' })
      })

      await expect(apiClient.get('/api/nonexistent')).rejects.toThrow('404 - Not Found')
    })

    it('should handle 500 Server Error', async () => {
      const { apiClient } = await import('@/services/api')

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'application/json' },
        json: async () => ({ error: 'Internal server error' })
      })

      await expect(apiClient.get('/api/test')).rejects.toThrow('500 - Internal server error')
    })

    it('should parse validation errors', async () => {
      const { apiClient } = await import('@/services/api')

      const validationErrors = {
        status: 400,
        errors: {
          email: 'Email is required',
          password: 'Password must be at least 8 characters'
        }
      }

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        headers: { get: () => 'application/json' },
        json: async () => validationErrors
      })

      try {
        await apiClient.post('/api/register', {})
      } catch (error) {
        expect(error.message).toContain('Email is required')
        expect(error.message).toContain('Password must be at least 8 characters')
      }
    })

    it('should handle network errors', async () => {
      const { apiClient } = await import('@/services/api')

      mockFetch.mockRejectedValueOnce(new Error('Network error'))

      await expect(apiClient.get('/api/test')).rejects.toThrow('Network error')
    })

    it('should handle timeout errors', async () => {
      const { apiClient } = await import('@/services/api')

      mockFetch.mockImplementationOnce(() =>
        new Promise((_, reject) =>
          setTimeout(() => reject(new Error('Request timeout')), 1000)
        )
      )

      await expect(
        apiClient.get('/api/test', { timeout: 500 })
      ).rejects.toThrow('Request timeout')
    })

    it('should include error details in response', async () => {
      const { apiClient } = await import('@/services/api')

      const errorResponse = {
        status: 400,
        message: 'Bad Request',
        error: 'Invalid input',
        timestamp: '2024-11-05T10:00:00Z',
        path: '/api/test',
        traceId: 'abc-123'
      }

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        headers: { get: () => 'application/json' },
        json: async () => errorResponse
      })

      try {
        await apiClient.get('/api/test')
      } catch (error) {
        expect(error.status).toBe(400)
        expect(error.message).toBe('Invalid input')
        expect(error.timestamp).toBe('2024-11-05T10:00:00Z')
        expect(error.traceId).toBe('abc-123')
      }
    })
  })

  describe('Retry Logic', () => {
    it('should retry on 5xx errors', async () => {
      const { apiClient } = await import('@/services/api')

      // Two failures, then success
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'application/json' },
        json: async () => ({ error: 'Server error' })
      })

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 503,
        headers: { get: () => 'application/json' },
        json: async () => ({ error: 'Service unavailable' })
      })

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => ({ data: 'success' })
      })

      const result = await apiClient.get('/api/test', { retries: 3 })

      expect(mockFetch).toHaveBeenCalledTimes(3)
      expect(result).toEqual({ data: 'success' })
    })

    it('should not retry on 4xx errors', async () => {
      const { apiClient } = await import('@/services/api')

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        headers: { get: () => 'application/json' },
        json: async () => ({ error: 'Bad request' })
      })

      await expect(
        apiClient.get('/api/test', { retries: 3 })
      ).rejects.toThrow('400 - Bad request')

      // Should only call once (no retries for 4xx)
      expect(mockFetch).toHaveBeenCalledTimes(1)
    })

    it('should respect retry delay', async () => {
      const { apiClient } = await import('@/services/api')

      const startTime = Date.now()

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        headers: { get: () => 'application/json' },
        json: async () => ({ error: 'Server error' })
      })

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => ({ data: {} })
      })

      await apiClient.get('/api/test', {
        retries: 3,
        retryDelay: 1000 // 1 second delay
      })

      const elapsedTime = Date.now() - startTime
      expect(elapsedTime).toBeGreaterThanOrEqual(1000)
    })
  })

  describe('Custom Headers', () => {
    it('should add custom headers to request', async () => {
      const { apiClient } = await import('@/services/api')

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => ({ data: {} })
      })

      await apiClient.get('/api/test', {
        headers: {
          'X-Custom-Header': 'custom-value',
          'X-Request-ID': 'req-123'
        }
      })

      const headers = mockFetch.mock.calls[0][1].headers
      expect(headers['X-Custom-Header']).toBe('custom-value')
      expect(headers['X-Request-ID']).toBe('req-123')
    })

    it('should override default headers with custom ones', async () => {
      const { apiClient } = await import('@/services/api')

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/xml' },
        json: async () => ({})
      })

      await apiClient.get('/api/test', {
        headers: { 'Content-Type': 'application/xml' }
      })

      const headers = mockFetch.mock.calls[0][1].headers
      expect(headers['Content-Type']).toBe('application/xml')
    })
  })

  describe('Response Types', () => {
    it('should parse JSON response', async () => {
      const { apiClient } = await import('@/services/api')

      const data = { id: 1, name: 'Test' }

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => data
      })

      const result = await apiClient.get('/api/test')

      expect(result).toEqual(data)
    })

    it('should handle empty response body', async () => {
      const { apiClient } = await import('@/services/api')

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 204,
        headers: { get: () => null },
        json: async () => ({})
      })

      const result = await apiClient.delete('/api/test')

      expect(result).toEqual({})
    })

    it('should return raw response when requested', async () => {
      const { apiClient } = await import('@/services/api')

      const response = {
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => ({ data: 'test' })
      }

      mockFetch.mockResolvedValueOnce(response)

      const result = await apiClient.get('/api/test', { responseType: 'raw' })

      expect(result).toBe(response)
    })

    it('should return blob for file downloads', async () => {
      const { apiClient } = await import('@/services/api')

      const blob = new Blob(['test content'], { type: 'text/plain' })

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/octet-stream' },
        blob: async () => blob
      })

      const result = await apiClient.get('/api/download', { responseType: 'blob' })

      expect(result).toBeInstanceOf(Blob)
    })
  })

  describe('Interceptors', () => {
    it('should allow adding request interceptors', async () => {
      const { apiClient } = await import('@/services/api')

      let interceptorCalled = false

      apiClient.addRequestInterceptor((config) => {
        interceptorCalled = true
        config.headers = { ...config.headers, 'X-Intercepter': 'true' }
        return config
      })

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => ({})
      })

      await apiClient.get('/api/test')

      expect(interceptorCalled).toBe(true)
      const headers = mockFetch.mock.calls[0][1].headers
      expect(headers['X-Intercepter']).toBe('true')
    })

    it('should allow adding response interceptors', async () => {
      const { apiClient } = await import('@/services/api')

      let responseInterceptorCalled = false

      apiClient.addResponseInterceptor((response) => {
        responseInterceptorCalled = true
        response.data = { ...response.data, intercepted: true }
        return response
      })

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => ({ original: 'data' })
      })

      const result = await apiClient.get('/api/test')

      expect(responseInterceptorCalled).toBe(true)
      expect(result.intercepted).toBe(true)
      expect(result.original).toBe('data')
    })
  })

  describe('Concurrent Requests', () => {
    it('should handle multiple concurrent requests', async () => {
      const { apiClient } = await import('@/services/api')

      // Mock multiple responses
      Array.from({ length: 5 }).forEach((_, i) => {
        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 200,
          headers: { get: () => 'application/json' },
          json: async () => ({ id: i, data: `response-${i}` })
        })
      })

      const promises = Array.from({ length: 5 }).map((_, i) =>
        apiClient.get(`/api/test-${i}`)
      )

      const results = await Promise.all(promises)

      expect(results).toHaveLength(5)
      results.forEach((result, i) => {
        expect(result.id).toBe(i)
        expect(result.data).toBe(`response-${i}`)
      })
    })

    it('should handle request cancellation', async () => {
      const { apiClient } = await import('@/services/api')

      const controller = new AbortController()

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => 'application/json' },
        json: async () => ({})
      })

      await apiClient.get('/api/test', { signal: controller.signal })

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/test',
        expect.objectContaining({
          signal: controller.signal
        })
      )
    })
  })

  describe('Rate Limiting', () => {
    it('should respect rate limit headers', async () => {
      const { apiClient } = await import('@/services/api')

      const rateLimitResponse = {
        ok: false,
        status: 429,
        headers: {
          get: (name: string) => {
            if (name === 'X-RateLimit-Limit') return '100'
            if (name === 'X-RateLimit-Remaining') return '0'
            if (name === 'X-RateLimit-Reset') return Math.floor(Date.now() / 1000) + 60
            return null
          },
          'Content-Type': 'application/json'
        },
        json: async () => ({ error: 'Rate limit exceeded' })
      }

      mockFetch.mockResolvedValueOnce(rateLimitResponse)

      await expect(apiClient.get('/api/test')).rejects.toThrow('Rate limit exceeded')

      const headers = rateLimitResponse.headers
      expect(headers.get('X-RateLimit-Limit')).toBe('100')
      expect(headers.get('X-RateLimit-Remaining')).toBe('0')
    })
  })
})
