/**
 * File Upload Helper Utilities
 *
 * Provides utilities for handling file uploads in tests
 * - Creating test files
 * - Uploading files
 * - Handling different file types
 */

import * as fs from 'fs'
import * as path from 'path'
import { type Page, type FilePayload } from '@playwright/test'

export interface FileConfig {
  name: string
  mimeType: string
  content: string | Buffer
}

export class FileUploadHelper {
  /**
   * Create a temporary test file
   */
  static createTestFile(
    config: FileConfig,
    directory: string = 'test-results/uploads'
  ): string {
    // Create directory if it doesn't exist
    if (!fs.existsSync(directory)) {
      fs.mkdirSync(directory, { recursive: true })
    }

    const filePath = path.join(directory, config.name)

    // Write file
    if (typeof config.content === 'string') {
      fs.writeFileSync(filePath, config.content)
    } else {
      fs.writeFileSync(filePath, config.content)
    }

    return filePath
  }

  /**
   * Create CSV test file
   */
  static createCSVFile(
    filename: string,
    data: any[],
    directory: string = 'test-results/uploads'
  ): string {
    if (!data || data.length === 0) {
      throw new Error('Data array is empty')
    }

    // Generate CSV headers from first object
    const headers = Object.keys(data[0])
    const csvContent = [
      headers.join(','),
      ...data.map(row =>
        headers.map(header => row[header]).join(',')
      )
    ].join('\n')

    return this.createTestFile({
      name: filename.endsWith('.csv') ? filename : `${filename}.csv`,
      mimeType: 'text/csv',
      content: csvContent
    }, directory)
  }

  /**
   * Create JSON test file
   */
  static createJSONFile(
    filename: string,
    data: any,
    directory: string = 'test-results/uploads'
  ): string {
    const jsonContent = JSON.stringify(data, null, 2)
    return this.createTestFile({
      name: filename.endsWith('.json') ? filename : `${filename}.json`,
      mimeType: 'application/json',
      content: jsonContent
    }, directory)
  }

  /**
   * Create text file
   */
  static createTextFile(
    filename: string,
    content: string,
    directory: string = 'test-results/uploads'
  ): string {
    return this.createTestFile({
      name: filename.endsWith('.txt') ? filename : `${filename}.txt`,
      mimeType: 'text/plain',
      content
    }, directory)
  }

  /**
   * Upload file to input element
   */
  static async uploadFile(
    page: Page,
    selector: string,
    filePath: string
  ): Promise<void> {
    await page.setInputFiles(selector, filePath)
  }

  /**
   * Upload multiple files
   */
  static async uploadFiles(
    page: Page,
    selector: string,
    filePaths: string[]
  ): Promise<void> {
    await page.setInputFiles(selector, filePaths)
  }

  /**
   * Upload file using drag and drop
   */
  static async uploadFileViaDragDrop(
    page: Page,
    filePath: string,
    targetSelector: string
  ): Promise<void> {
    // Create file chooser
    const [fileChooser] = await Promise.all([
      page.waitForEvent('filechooser'),
      page.click(targetSelector)
    ])

    await fileChooser.setFiles(filePath)
  }

  /**
   * Verify file upload
   */
  static async verifyFileUpload(
    page: Page,
    expectedFilename: string
  ): Promise<boolean> {
    // Check for success message
    const successMessage = await page.locator(
      '[data-testid="upload-success"], .success:has-text("upload")'
    ).isVisible()

    // Check for file name in UI
    const fileName = await page.locator(
      '[data-testid="uploaded-filename"], .uploaded-file'
    ).textContent()

    return successMessage && fileName?.includes(expectedFilename) || false
  }

  /**
   * Create customer import file
   */
  static createCustomerImportFile(
    filename: string = 'customer-import-test.csv',
    directory: string = 'test-results/uploads'
  ): string {
    const customerData = [
      { firstName: 'John', lastName: 'Doe', email: 'john.doe@test.com', phone: '+1-555-0100' },
      { firstName: 'Jane', lastName: 'Smith', email: 'jane.smith@test.com', phone: '+1-555-0101' },
      { firstName: 'Bob', lastName: 'Johnson', email: 'bob.johnson@test.com', phone: '+1-555-0102' }
    ]

    return this.createCSVFile(filename, customerData, directory)
  }

  /**
   * Create product import file
   */
  static createProductImportFile(
    filename: string = 'product-import-test.csv',
    directory: string = 'test-results/uploads'
  ): string {
    const productData = [
      { name: 'Basic Plan', description: 'Basic service plan', price: '29.99', currency: 'USD' },
      { name: 'Pro Plan', description: 'Professional service plan', price: '59.99', currency: 'USD' },
      { name: 'Enterprise Plan', description: 'Enterprise service plan', price: '199.99', currency: 'USD' }
    ]

    return this.createCSVFile(filename, productData, directory)
  }

  /**
   * Create large CSV file for performance testing
   */
  static createLargeCSVFile(
    filename: string,
    rowCount: number,
    directory: string = 'test-results/uploads'
  ): string {
    const data = Array.from({ length: rowCount }, (_, i) => ({
      name: `Customer ${i + 1}`,
      email: `customer${i + 1}@example.com`,
      phone: `+1-555-${String(i + 1000).padStart(4, '0')}`,
      status: i % 3 === 0 ? 'active' : i % 3 === 1 ? 'pending' : 'inactive'
    }))

    return this.createCSVFile(filename, data, directory)
  }

  /**
   * Clean up test files
   */
  static cleanup(directory: string = 'test-results/uploads'): void {
    if (fs.existsSync(directory)) {
      fs.rmSync(directory, { recursive: true, force: true })
    }
  }

  /**
   * Get file info
   */
  static getFileInfo(filePath: string): {
    size: number
    extension: string
    basename: string
  } {
    const stats = fs.statSync(filePath)
    const ext = path.extname(filePath)
    const basename = path.basename(filePath)

    return {
      size: stats.size,
      extension: ext,
      basename
    }
  }
}
