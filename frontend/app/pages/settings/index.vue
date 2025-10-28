<template>
  <div class="settings-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Settings</h1>
        <p class="page-subtitle">Configure your BSS system preferences</p>
      </div>
    </div>

    <!-- Settings Sections -->
    <div class="settings-content">
      <!-- User Profile Section -->
      <div class="settings-section">
        <div class="section-header">
          <h2>User Profile</h2>
          <p>Manage your account information and preferences</p>
        </div>
        <div class="section-content">
          <div class="profile-card">
            <div class="profile-avatar">
              <span class="avatar-placeholder">👤</span>
            </div>
            <div class="profile-info">
              <h3>Current User</h3>
              <p class="profile-email">john.doe@example.com</p>
              <p class="profile-role">System Administrator</p>
            </div>
            <div class="profile-actions">
              <AppButton variant="secondary" size="sm" disabled>
                Edit Profile
              </AppButton>
            </div>
          </div>
        </div>
      </div>

      <!-- System Settings Section -->
      <div class="settings-section">
        <div class="section-header">
          <h2>System Configuration</h2>
          <p>Configure global system settings and preferences</p>
        </div>
        <div class="section-content">
          <div class="settings-grid">
            <div class="setting-item">
              <div class="setting-item__info">
                <h4>Theme Preference</h4>
                <p>Choose your preferred color theme</p>
              </div>
              <div class="setting-item__control">
                <AppSelect
                  :model-value="'pastel'"
                  :options="themeOptions"
                  size="sm"
                  disabled
                />
              </div>
            </div>
            
            <div class="setting-item">
              <div class="setting-item__info">
                <h4>Language</h4>
                <p>Select your preferred language</p>
              </div>
              <div class="setting-item__control">
                <AppSelect
                  :model-value="'en'"
                  :options="languageOptions"
                  size="sm"
                  disabled
                />
              </div>
            </div>
            
            <div class="setting-item">
              <div class="setting-item__info">
                <h4>Timezone</h4>
                <p>Set your local timezone</p>
              </div>
              <div class="setting-item__control">
                <AppSelect
                  :model-value="'UTC'"
                  :options="timezoneOptions"
                  size="sm"
                  disabled
                />
              </div>
            </div>
            
            <div class="setting-item">
              <div class="setting-item__info">
                <h4>Date Format</h4>
                <p>Choose how dates are displayed</p>
              </div>
              <div class="setting-item__control">
                <AppSelect
                  :model-value="'MM/DD/YYYY'"
                  :options="dateFormatOptions"
                  size="sm"
                  disabled
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Notification Settings Section -->
      <div class="settings-section">
        <div class="section-header">
          <h2>Notifications</h2>
          <p>Configure how and when you receive notifications</p>
        </div>
        <div class="section-content">
          <div class="settings-list">
            <div class="setting-item setting-item--toggle">
              <div class="setting-item__info">
                <h4>Email Notifications</h4>
                <p>Receive email notifications for important events</p>
              </div>
              <div class="setting-item__control">
                <div class="toggle-switch toggle-switch--disabled">
                  <input type="checkbox" checked disabled />
                  <span class="toggle-slider"></span>
                </div>
              </div>
            </div>
            
            <div class="setting-item setting-item--toggle">
              <div class="setting-item__info">
                <h4>Browser Notifications</h4>
                <p>Show browser notifications for real-time updates</p>
              </div>
              <div class="setting-item__control">
                <div class="toggle-switch toggle-switch--disabled">
                  <input type="checkbox" disabled />
                  <span class="toggle-slider"></span>
                </div>
              </div>
            </div>
            
            <div class="setting-item setting-item--toggle">
              <div class="setting-item__info">
                <h4>Customer Updates</h4>
                <p>Get notified when customer information changes</p>
              </div>
              <div class="setting-item__control">
                <div class="toggle-switch toggle-switch--disabled">
                  <input type="checkbox" checked disabled />
                  <span class="toggle-slider"></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- System Information Section -->
      <div class="settings-section">
        <div class="section-header">
          <h2>System Information</h2>
          <p>View system status and technical information</p>
        </div>
        <div class="section-content">
          <div class="system-info-grid">
            <div class="info-item">
              <label>Application Version</label>
              <span>v1.0.0</span>
            </div>
            <div class="info-item">
              <label>Frontend Framework</label>
              <span>Nuxt 4.2.0</span>
            </div>
            <div class="info-item">
              <label>Build Date</label>
              <span>{{ currentDate }}</span>
            </div>
            <div class="info-item">
              <label>Environment</label>
              <span class="status-badge status-badge--development">Development</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// Page meta
definePageMeta({
  title: 'Settings'
})

// Reactive data
const currentDate = new Date().toLocaleDateString('en-US', {
  year: 'numeric',
  month: 'long',
  day: 'numeric'
})

// Settings options
const themeOptions = [
  { value: 'pastel', label: 'Pastel (Current)' },
  { value: 'dark', label: 'Dark Mode' },
  { value: 'light', label: 'Light Mode' },
  { value: 'auto', label: 'Auto (System)' }
]

const languageOptions = [
  { value: 'en', label: 'English' },
  { value: 'pl', label: 'Polski' },
  { value: 'de', label: 'Deutsch' },
  { value: 'fr', label: 'Français' }
]

const timezoneOptions = [
  { value: 'UTC', label: 'UTC (Coordinated Universal Time)' },
  { value: 'Europe/Warsaw', label: 'Europe/Warsaw' },
  { value: 'America/New_York', label: 'America/New_York' },
  { value: 'Asia/Tokyo', label: 'Asia/Tokyo' }
]

const dateFormatOptions = [
  { value: 'MM/DD/YYYY', label: 'MM/DD/YYYY' },
  { value: 'DD/MM/YYYY', label: 'DD/MM/YYYY' },
  { value: 'YYYY-MM-DD', label: 'YYYY-MM-DD' },
  { value: 'DD MMM YYYY', label: 'DD MMM YYYY' }
]
</script>

<style scoped>
.settings-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  max-width: 800px;
}

/* Page Header */
.page-header {
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.page-title {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.page-subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

/* Settings Content */
.settings-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}

/* Settings Sections */
.settings-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.section-header {
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-alt);
}

.section-header h2 {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.section-header p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.section-content {
  padding: var(--space-6);
}

/* Profile Card */
.profile-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface-alt);
}

.profile-avatar {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-full);
  background: var(--color-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-placeholder {
  font-size: var(--font-size-xl);
}

.profile-info {
  flex: 1;
}

.profile-info h3 {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.profile-email,
.profile-role {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.profile-role {
  font-weight: var(--font-weight-medium);
}

.profile-actions {
  flex-shrink: 0;
}

/* Settings Grid */
.settings-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-alt);
}

.setting-item__info {
  flex: 1;
}

.setting-item__info h4 {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.setting-item__info p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.setting-item__control {
  flex-shrink: 0;
}

/* Settings List */
.settings-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.setting-item--toggle {
  padding: var(--space-4);
}

/* Toggle Switch */
.toggle-switch {
  position: relative;
  width: 48px;
  height: 24px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: var(--color-border);
  border-radius: 24px;
  transition: var(--transition-base) var(--transition-timing);
}

.toggle-slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: var(--color-surface);
  border-radius: 50%;
  transition: var(--transition-base) var(--transition-timing);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.toggle-switch input:checked + .toggle-slider {
  background-color: var(--color-primary);
}

.toggle-switch input:checked + .toggle-slider:before {
  transform: translateX(24px);
}

.toggle-switch--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toggle-switch--disabled .toggle-slider {
  cursor: not-allowed;
}

/* System Information */
.system-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-alt);
}

.info-item label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-item span {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.status-badge--development {
  background: var(--color-warning-light);
  color: var(--color-warning);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .settings-page {
    max-width: none;
  }
  
  .page-title {
    font-size: var(--font-size-2xl);
  }
  
  .section-content {
    padding: var(--space-4);
  }
  
  .profile-card {
    flex-direction: column;
    text-align: center;
    gap: var(--space-3);
  }
  
  .setting-item {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
  }
  
  .setting-item__control {
    width: 100%;
  }
  
  .setting-item__control .app-select {
    width: 100%;
  }
  
  .system-info-grid {
    grid-template-columns: 1fr;
  }
}

/* Tablet adjustments */
@media (min-width: 769px) and (max-width: 1024px) {
  .profile-card {
    gap: var(--space-3);
  }
  
  .system-info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
