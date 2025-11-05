<template>
  <div class="import-cdr-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <NuxtLink to="/billing/usage-records" class="back-link">
          ← Back to Usage Records
        </NuxtLink>
        <h1 class="page-title">Import CDR Files</h1>
        <p class="page-subtitle">
          Upload and process Call Detail Record (CDR) files for usage tracking
        </p>
      </div>
    </div>

    <!-- Upload Section -->
    <div class="upload-section">
      <div class="upload-card">
        <div class="upload-card__header">
          <div class="upload-icon">
            <i class="pi pi-upload"></i>
          </div>
          <h2>Upload CDR Files</h2>
          <p>Select CDR files to import. Supported formats: CSV, XML, JSON</p>
        </div>

        <div class="upload-card__content">
          <!-- File Drop Zone -->
          <div
            class="file-drop-zone"
            :class="{ 'dragover': isDragOver }"
            @dragenter.prevent="isDragOver = true"
            @dragover.prevent="isDragOver = true"
            @dragleave.prevent="isDragOver = false"
            @drop.prevent="handleDrop"
          >
            <input
              ref="fileInput"
              type="file"
              multiple
              accept=".csv,.xml,.json"
              @change="handleFileSelect"
              style="display: none"
            />
            <div class="drop-zone-content">
              <i class="pi pi-cloud-upload drop-zone-icon"></i>
              <h3>Drag & Drop Files Here</h3>
              <p>or</p>
              <Button
                label="Browse Files"
                icon="pi pi-folder-open"
                variant="primary"
                @click="triggerFileInput"
              />
            </div>
          </div>

          <!-- Selected Files List -->
          <div v-if="selectedFiles.length > 0" class="selected-files">
            <h3>Selected Files ({{ selectedFiles.length }})</h3>
            <div class="files-list">
              <div
                v-for="(file, index) in selectedFiles"
                :key="index"
                class="file-item"
              >
                <div class="file-info">
                  <i class="pi pi-file file-icon"></i>
                  <div class="file-details">
                    <div class="file-name">{{ file.name }}</div>
                    <div class="file-meta">
                      {{ formatFileSize(file.size) }} • {{ getFileType(file.name) }}
                    </div>
                  </div>
                </div>
                <div class="file-actions">
                  <Button
                    icon="pi pi-times"
                    text
                    rounded
                    severity="danger"
                    @click="removeFile(index)"
                    v-tooltip.top="'Remove file'"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- Import Options -->
          <div class="import-options">
            <h3>Import Options</h3>
            <div class="options-grid">
              <div class="option-item">
                <label class="option-label">
                  <input
                    type="checkbox"
                    v-model="importOptions.skipDuplicates"
                  />
                  <span>Skip duplicate records</span>
                </label>
                <p class="option-description">
                  Skip records that already exist in the database
                </p>
              </div>

              <div class="option-item">
                <label class="option-label">
                  <input
                    type="checkbox"
                    v-model="importOptions.validateOnly"
                  />
                  <span>Validation only</span>
                </label>
                <p class="option-description">
                  Validate files without importing (dry run)
                </p>
              </div>

              <div class="option-item">
                <label class="option-label">
                  <input
                    type="checkbox"
                    v-model="importOptions.autoRate"
                  />
                  <span>Auto-rate after import</span>
                </label>
                <p class="option-description">
                  Automatically rate usage records after successful import
                </p>
              </div>

              <div class="option-item">
                <label class="option-label">
                  <span>Date Format</span>
                </label>
                <Dropdown
                  v-model="importOptions.dateFormat"
                  :options="dateFormatOptions"
                  optionLabel="label"
                  optionValue="value"
                  style="width: 100%"
                />
              </div>
            </div>
          </div>

          <!-- Import Button -->
          <div class="import-actions">
            <Button
              label="Cancel"
              variant="secondary"
              @click="handleCancel"
              :disabled="isImporting"
            />
            <Button
              :label="importOptions.validateOnly ? 'Validate Files' : 'Start Import'"
              icon="pi pi-play"
              variant="primary"
              @click="handleImport"
              :loading="isImporting"
              :disabled="selectedFiles.length === 0 || isImporting"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Import Progress -->
    <div v-if="isImporting" class="progress-section">
      <div class="progress-card">
        <div class="progress-card__header">
          <h2>Import Progress</h2>
          <div class="progress-status">
            <span class="progress-step">{{ currentStep }}</span>
            <span class="progress-percent">{{ importProgress }}%</span>
          </div>
        </div>

        <div class="progress-card__content">
          <div class="progress-bar-container">
            <div
              class="progress-bar"
              :style="{ width: importProgress + '%' }"
            ></div>
          </div>

          <div class="progress-details">
            <div class="progress-item">
              <span class="progress-label">Files Processed:</span>
              <span class="progress-value">{{ processedFiles }} / {{ selectedFiles.length }}</span>
            </div>
            <div class="progress-item">
              <span class="progress-label">Records Imported:</span>
              <span class="progress-value">{{ importedRecords }}</span>
            </div>
            <div class="progress-item">
              <span class="progress-label">Records Skipped:</span>
              <span class="progress-value">{{ skippedRecords }}</span>
            </div>
            <div class="progress-item">
              <span class="progress-label">Errors:</span>
              <span class="progress-value" :class="{ 'has-errors': errorCount > 0 }">
                {{ errorCount }}
              </span>
            </div>
          </div>

          <div v-if="importLog.length > 0" class="import-log">
            <h4>Import Log</h4>
            <div class="log-content">
              <div
                v-for="(log, index) in importLog"
                :key="index"
                class="log-item"
                :class="`log-${log.type}`"
              >
                <i :class="getLogIcon(log.type)"></i>
                <span>{{ log.message }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Import Summary -->
    <div v-if="importCompleted && !isImporting" class="summary-section">
      <div class="summary-card">
        <div class="summary-icon" :class="{ 'success': errorCount === 0, 'warning': errorCount > 0 }">
          <i :class="errorCount === 0 ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle'"></i>
        </div>
        <div class="summary-content">
          <h2>Import Completed</h2>
          <p class="summary-message">
            {{ errorCount === 0
              ? 'All files have been successfully imported'
              : `Import completed with ${errorCount} error(s)`
            }}
          </p>
          <div class="summary-stats">
            <div class="stat">
              <span class="stat-label">Total Files</span>
              <span class="stat-value">{{ selectedFiles.length }}</span>
            </div>
            <div class="stat">
              <span class="stat-label">Imported</span>
              <span class="stat-value">{{ importedRecords }}</span>
            </div>
            <div class="stat">
              <span class="stat-label">Skipped</span>
              <span class="stat-value">{{ skippedRecords }}</span>
            </div>
            <div class="stat">
              <span class="stat-label">Errors</span>
              <span class="stat-value" :class="{ 'has-errors': errorCount > 0 }">{{ errorCount }}</span>
            </div>
          </div>
        </div>
        <div class="summary-actions">
          <Button
            label="View Records"
            icon="pi pi-eye"
            variant="primary"
            @click="navigateTo('/billing/usage-records')"
          />
          <Button
            label="Import More"
            icon="pi pi-upload"
            variant="secondary"
            @click="resetImport"
          />
        </div>
      </div>
    </div>

    <!-- Toast for notifications -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { useBillingStore } from '~/stores/billing'

// Page meta
definePageMeta({
  title: 'Import CDR Files'
})

// Store
const billingStore = useBillingStore()
const { showToast } = useToast()

// Reactive state
const fileInput = ref<HTMLInputElement | null>(null)
const selectedFiles = ref<File[]>([])
const isDragOver = ref(false)
const isImporting = ref(false)
const importProgress = ref(0)
const currentStep = ref('')
const processedFiles = ref(0)
const importedRecords = ref(0)
const skippedRecords = ref(0)
const errorCount = ref(0)
const importLog = ref<Array<{ type: 'info' | 'success' | 'warning' | 'error'; message: string }>>([])
const importCompleted = ref(false)

// Import options
const importOptions = ref({
  skipDuplicates: true,
  validateOnly: false,
  autoRate: false,
  dateFormat: 'ISO'
})

// Date format options
const dateFormatOptions = [
  { label: 'ISO 8601 (2024-01-15T10:30:00)', value: 'ISO' },
  { label: 'YYYY-MM-DD HH:mm:ss', value: 'YYYY-MM-DD' },
  { label: 'DD/MM/YYYY HH:mm:ss', value: 'DD/MM/YYYY' },
  { label: 'MM/DD/YYYY HH:mm:ss', value: 'MM/DD/YYYY' }
]

// Methods
const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files) {
    addFiles(Array.from(target.files))
  }
}

const handleDrop = (event: DragEvent) => {
  isDragOver.value = false
  if (event.dataTransfer?.files) {
    addFiles(Array.from(event.dataTransfer.files))
  }
}

const addFiles = (files: File[]) => {
  const validFiles = files.filter(file => {
    const isValid = validateFile(file)
    if (!isValid) {
      showToast({
        severity: 'warn',
        summary: 'Invalid File',
        detail: `${file.name} is not a supported format`,
        life: 3000
      })
    }
    return isValid
  })

  selectedFiles.value.push(...validFiles)
}

const validateFile = (file: File): boolean => {
  const allowedTypes = ['text/csv', 'application/json', 'text/xml', 'application/xml']
  const allowedExtensions = ['.csv', '.json', '.xml']
  const fileName = file.name.toLowerCase()

  return (
    allowedTypes.includes(file.type) ||
    allowedExtensions.some(ext => fileName.endsWith(ext))
  )
}

const removeFile = (index: number) => {
  selectedFiles.value.splice(index, 1)
}

const handleCancel = () => {
  navigateTo('/billing/usage-records')
}

const handleImport = async () => {
  if (selectedFiles.value.length === 0) return

  isImporting.value = true
  importProgress.value = 0
  processedFiles.value = 0
  importedRecords.value = 0
  skippedRecords.value = 0
  errorCount.value = 0
  importLog.value = []
  importCompleted.value = false

  try {
    currentStep.value = 'Initializing...'
    updateProgress(5)

    for (let i = 0; i < selectedFiles.value.length; i++) {
      const file = selectedFiles.value[i]
      currentStep.value = `Processing ${file.name}...`
      updateProgress(5 + (i / selectedFiles.value.length) * 85)

      // Simulate file processing
      await simulateFileProcessing(file)

      processedFiles.value++
      addLog('success', `${file.name} processed successfully`)
    }

    currentStep.value = 'Finalizing...'
    updateProgress(100)

    importCompleted.value = true

    showToast({
      severity: 'success',
      summary: 'Import Completed',
      detail: `Successfully imported ${importedRecords.value} records`,
      life: 5000
    })

  } catch (error: any) {
    addLog('error', `Import failed: ${error.message}`)
    showToast({
      severity: 'error',
      summary: 'Import Failed',
      detail: error.message || 'An error occurred during import',
      life: 5000
    })
  } finally {
    isImporting.value = false
  }
}

const simulateFileProcessing = async (file: File): Promise<void> => {
  // Simulate processing delay
  await new Promise(resolve => setTimeout(resolve, 1000))

  // Simulate file size to record count
  const estimatedRecords = Math.floor(file.size / 100)
  importedRecords.value += Math.floor(estimatedRecords * 0.9) // 90% success rate
  skippedRecords.value += Math.floor(estimatedRecords * 0.1) // 10% skipped

  // Simulate occasional errors
  if (Math.random() > 0.9) {
    errorCount.value++
    addLog('warning', `Some records in ${file.name} had validation warnings`)
  }
}

const updateProgress = (value: number) => {
  importProgress.value = Math.min(100, Math.max(0, value))
}

const addLog = (type: 'info' | 'success' | 'warning' | 'error', message: string) => {
  importLog.value.push({ type, message })
}

const getLogIcon = (type: string): string => {
  const icons: Record<string, string> = {
    info: 'pi pi-info-circle',
    success: 'pi pi-check-circle',
    warning: 'pi pi-exclamation-triangle',
    error: 'pi pi-times-circle'
  }
  return icons[type] || 'pi pi-info-circle'
}

const resetImport = () => {
  selectedFiles.value = []
  importProgress.value = 0
  processedFiles.value = 0
  importedRecords.value = 0
  skippedRecords.value = 0
  errorCount.value = 0
  importLog.value = []
  importCompleted.value = false
  isImporting.value = false
}

// Utility functions
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const getFileType = (fileName: string): string => {
  const ext = fileName.split('.').pop()?.toUpperCase() || ''
  return ext
}
</script>

<style scoped>
.import-cdr-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  padding: var(--space-6);
  max-width: 1200px;
  margin: 0 auto;
}

/* Page Header */
.page-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: var(--font-size-sm);
  transition: color 0.2s;
}

.back-link:hover {
  color: var(--color-primary);
}

.page-title {
  margin: 0;
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.page-subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

/* Upload Section */
.upload-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.upload-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.upload-card__header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-6);
  text-align: center;
  background: var(--color-surface-elevated);
  border-bottom: 1px solid var(--color-border);
}

.upload-icon {
  width: 64px;
  height: 64px;
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
}

.upload-card__header h2 {
  margin: 0;
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.upload-card__header p {
  margin: 0;
  color: var(--color-text-secondary);
}

.upload-card__content {
  padding: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* File Drop Zone */
.file-drop-zone {
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-8);
  text-align: center;
  transition: all 0.3s ease;
  cursor: pointer;
}

.file-drop-zone:hover,
.file-drop-zone.dragover {
  border-color: var(--color-primary);
  background: var(--color-surface-elevated);
}

.drop-zone-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}

.drop-zone-icon {
  font-size: 3rem;
  color: var(--color-text-muted);
}

.drop-zone-content h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.drop-zone-content p {
  margin: 0;
  color: var(--color-text-secondary);
}

/* Selected Files */
.selected-files {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.selected-files h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.files-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  max-height: 300px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--color-surface-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.file-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex: 1;
}

.file-icon {
  font-size: 1.5rem;
  color: var(--color-text-secondary);
}

.file-details {
  flex: 1;
}

.file-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.file-meta {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Import Options */
.import-options {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.import-options h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.options-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-4);
}

.option-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.option-label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  cursor: pointer;
}

.option-label input[type="checkbox"] {
  cursor: pointer;
}

.option-description {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  padding-left: calc(var(--space-5));
}

/* Import Actions */
.import-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

/* Progress Section */
.progress-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.progress-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.progress-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4);
  background: var(--color-surface-elevated);
  border-bottom: 1px solid var(--color-border);
}

.progress-card__header h2 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.progress-status {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.progress-step {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.progress-percent {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

.progress-card__content {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.progress-bar-container {
  width: 100%;
  height: 8px;
  background: var(--color-surface-elevated);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: var(--color-primary);
  transition: width 0.3s ease;
}

.progress-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-3);
}

.progress-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.progress-value {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.progress-value.has-errors {
  color: var(--color-danger);
}

/* Import Log */
.import-log {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.import-log h4 {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.log-content {
  max-height: 200px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-2);
  background: var(--color-surface-elevated);
  border-radius: var(--radius-md);
}

.log-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
}

.log-item i {
  flex-shrink: 0;
}

.log-info i {
  color: var(--color-info);
}

.log-success i {
  color: var(--color-success);
}

.log-warning i {
  color: var(--color-warning);
}

.log-error i {
  color: var(--color-danger);
}

/* Summary Section */
.summary-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.summary-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
  text-align: center;
}

.summary-icon {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
}

.summary-icon.success {
  background: var(--color-success);
  color: white;
}

.summary-icon.warning {
  background: var(--color-warning);
  color: white;
}

.summary-content {
  flex: 1;
}

.summary-content h2 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.summary-message {
  margin: 0 0 var(--space-4) 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.stat {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  font-weight: var(--font-weight-medium);
}

.stat-value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.stat-value.has-errors {
  color: var(--color-danger);
}

.summary-actions {
  display: flex;
  gap: var(--space-3);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .import-cdr-page {
    padding: var(--space-4);
  }

  .upload-card__header {
    padding: var(--space-4);
  }

  .upload-card__content {
    padding: var(--space-4);
  }

  .options-grid {
    grid-template-columns: 1fr;
  }

  .import-actions {
    flex-direction: column;
  }

  .import-actions .p-button {
    width: 100%;
  }

  .progress-details {
    grid-template-columns: 1fr;
  }

  .summary-actions {
    flex-direction: column;
    width: 100%;
  }

  .summary-actions .p-button {
    width: 100%;
  }
}
</style>
