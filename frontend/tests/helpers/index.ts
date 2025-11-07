/**
 * Test Helpers and Utilities - Central Export
 *
 * Comprehensive collection of test helper utilities
 * Provides reusable functions for common test operations
 *
 * Usage:
 * ```typescript
 * import { AuthHelper, NetworkHelper, DateHelper } from '@/tests/helpers'
 *
 * await AuthHelper.login(page, { username: 'user', password: 'pass' })
 * await NetworkHelper.mockApiRequests(page, mocks)
 * const date = DateHelper.addDays(new Date(), 7)
 * ```
 */

// Authentication helpers
export { AuthHelper, type AuthConfig } from './auth.helper'

// Network and API mocking
export { NetworkHelper, type MockRequest, type MockResponse } from './network.helper'

// Date and time utilities
export { DateHelper } from './date.helper'

// File upload utilities
export { FileUploadHelper, type FileConfig } from './file-upload.helper'

// Console message capture
export { ConsoleHelper, type ConsoleMessage } from './console.helper'

// Error handling utilities
export { ErrorHelper, type ErrorAssertion } from './error.helper'
