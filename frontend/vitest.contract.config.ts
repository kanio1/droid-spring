/// <reference types="vitest" />
import { defineConfig } from 'vitest/config'
import { resolve } from 'path'

export default defineConfig({
  test: {
    globals: true,
    environment: 'node',
    testTimeout: 60000,
    hookTimeout: 30000,
    teardownTimeout: 10000,
    include: ['tests/contract/**/*.test.{ts,js}'],
    exclude: [
      'node_modules',
      'dist',
      '.git',
      '.cache'
    ],
    reporters: [
      'default',
      'json',
      'html'
    ],
    outputFile: {
      json: './test-results/contract-results.json',
      html: './test-results/contract-report.html'
    },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html', 'lcov'],
      exclude: [
        'node_modules/',
        'tests/contract/consumers/*.test.ts',
        '**/*.d.ts',
        '**/*.config.*',
        '**/coverage/**',
        '**/dist/**'
      ]
    }
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, './app'),
      '@tests': resolve(__dirname, './tests')
    }
  }
})
