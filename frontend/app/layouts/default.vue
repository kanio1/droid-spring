<template>
  <div class="app-layout">
    <!-- Sidebar Navigation -->
    <aside class="sidebar">
      <div class="sidebar__header">
        <div class="sidebar__logo">
          <Icon name="lucide:building-2" :size="24" class="sidebar__logo-icon" />
          <span class="sidebar__logo-text">BSS Portal</span>
        </div>
      </div>
      
      <nav class="sidebar__nav" aria-label="Main navigation">
        <ul class="sidebar__nav-list">
          <li class="sidebar__nav-item">
            <NuxtLink to="/" class="sidebar__nav-link" exact-active-class="sidebar__nav-link--active">
              <Icon name="lucide:bar-chart" :size="20" class="sidebar__nav-icon" />
              <span class="sidebar__nav-text">Dashboard</span>
            </NuxtLink>
          </li>
          <li class="sidebar__nav-item">
            <NuxtLink to="/customers" class="sidebar__nav-link" exact-active-class="sidebar__nav-link--active">
              <Icon name="lucide:users" :size="20" class="sidebar__nav-icon" />
              <span class="sidebar__nav-text">Customers</span>
            </NuxtLink>
          </li>
          <li class="sidebar__nav-item">
            <NuxtLink to="/addresses" class="sidebar__nav-link" exact-active-class="sidebar__nav-link--active">
              <Icon name="lucide:map-pin" :size="20" class="sidebar__nav-icon" />
              <span class="sidebar__nav-text">Addresses</span>
            </NuxtLink>
          </li>
          <li class="sidebar__nav-item">
            <NuxtLink to="/coverage-nodes" class="sidebar__nav-link" exact-active-class="sidebar__nav-link--active">
              <Icon name="lucide:map" :size="20" class="sidebar__nav-icon" />
              <span class="sidebar__nav-text">Coverage Nodes</span>
            </NuxtLink>
          </li>
          <li class="sidebar__nav-item">
            <NuxtLink to="/monitoring" class="sidebar__nav-link" exact-active-class="sidebar__nav-link--active">
              <Icon name="lucide:activity" :size="20" class="sidebar__nav-icon" />
              <span class="sidebar__nav-text">Monitoring</span>
            </NuxtLink>
          </li>
          <li class="sidebar__nav-item">
            <NuxtLink to="/monitoring/alerts" class="sidebar__nav-link" exact-active-class="sidebar__nav-link--active">
              <Icon name="lucide:alert-triangle" :size="20" class="sidebar__nav-icon" />
              <span class="sidebar__nav-text">Alerts</span>
            </NuxtLink>
          </li>
        </ul>
        
        <div class="sidebar__divider"></div>
        
        <ul class="sidebar__nav-list">
          <li class="sidebar__nav-item">
            <NuxtLink to="/settings" class="sidebar__nav-link" exact-active-class="sidebar__nav-link--active">
              <Icon name="lucide:settings" :size="20" class="sidebar__nav-icon" />
              <span class="sidebar__nav-text">Settings</span>
            </NuxtLink>
          </li>
          <li class="sidebar__nav-item">
            <NuxtLink to="/profile" class="sidebar__nav-link" exact-active-class="sidebar__nav-link--active">
              <Icon name="lucide:user" :size="20" class="sidebar__nav-icon" />
              <span class="sidebar__nav-text">Profile</span>
            </NuxtLink>
          </li>
        </ul>
      </nav>
    </aside>

    <!-- Main Content Area -->
    <div class="main-content">
      <!-- Top Header -->
      <header class="top-header">
        <div class="top-header__left">
          <h1 class="top-header__title">{{ pageTitle }}</h1>
        </div>
        
        <div class="top-header__right">
          <!-- Theme Toggle -->
          <button class="top-header__theme-toggle" @click="toggleTheme" :title="`Switch to ${nextTheme} mode`">
            <Icon :name="actualTheme === 'dark' ? 'lucide:moon' : 'lucide:sun'" :size="20" class="top-header__theme-icon" />
          </button>

          <div class="top-header__user">
            <span class="top-header__user-name">{{ userName }}</span>
            <button class="top-header__user-menu" @click="toggleUserMenu">
              <span class="top-header__user-avatar">{{ userInitials }}</span>
            </button>
          </div>
        </div>
      </header>

      <!-- Page Content -->
      <main class="page-content">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import Icon from '~/components/ui/Icon.vue'

// Theme management
const { actualTheme, toggleTheme } = useTheme()
const nextTheme = computed(() => actualTheme.value === 'dark' ? 'light' : 'dark')

// Authentication
const { profile } = useAuth()

// User info from auth profile
const userName = computed(() => {
  if (profile.value?.firstName && profile.value?.lastName) {
    return `${profile.value.firstName} ${profile.value.lastName}`
  }
  if (profile.value?.username) {
    return profile.value.username
  }
  return 'Anonymous User'
})

const userInitials = computed(() => {
  if (profile.value?.firstName && profile.value?.lastName) {
    return `${profile.value.firstName.charAt(0)}${profile.value.lastName.charAt(0)}`.toUpperCase()
  }
  if (profile.value?.username) {
    return profile.value.username.substring(0, 2).toUpperCase()
  }
  return 'AU'
})

// Page title based on route
const route = useRoute()
const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    '/': 'Dashboard',
    '/customers': 'Customers',
    '/addresses': 'Addresses',
    '/coverage-nodes': 'Coverage Nodes',
    '/monitoring': 'Resource Monitoring',
    '/monitoring/alerts': 'Alert Management',
    '/settings': 'Settings',
    '/profile': 'Profile'
  }
  return titles[route.path] || 'BSS Portal'
})

// User menu toggle
const showUserMenu = ref(false)
const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

// Close menu when clicking outside
onClickOutside(useTemplateRef('userMenu'), () => {
  showUserMenu.value = false
})
</script>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-background);
}

/* Sidebar */
.sidebar {
  width: var(--sidebar-width);
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  position: fixed;
  height: 100vh;
  left: 0;
  top: 0;
  z-index: var(--z-fixed);
}

.sidebar__header {
  padding: var(--space-6) var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.sidebar__logo {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.sidebar__logo-icon {
  font-size: var(--font-size-xl);
}

.sidebar__logo-text {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.sidebar__nav {
  flex: 1;
  padding: var(--space-4) 0;
}

.sidebar__nav-list {
  padding: 0 var(--space-2);
}

.sidebar__nav-item {
  margin-bottom: var(--space-1);
}

.sidebar__nav-link {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  color: var(--color-text-secondary);
  text-decoration: none;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast) var(--transition-timing);
  font-weight: var(--font-weight-medium);
}

.sidebar__nav-link:hover {
  background: var(--color-surface-alt);
  color: var(--color-text-primary);
}

.sidebar__nav-link--active {
  background: var(--color-primary-light);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-semibold);
}

.sidebar__nav-icon {
  font-size: var(--font-size-lg);
  width: 20px;
  text-align: center;
}

.sidebar__nav-text {
  font-size: var(--font-size-sm);
}

.sidebar__divider {
  height: 1px;
  background: var(--color-border);
  margin: var(--space-4) var(--space-4);
}

/* Main Content */
.main-content {
  flex: 1;
  margin-left: var(--sidebar-width);
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* Top Header */
.top-header {
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  padding: 0 var(--space-6);
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
}

.top-header__title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.top-header__user {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.top-header__user-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.top-header__user-menu {
  padding: var(--space-1);
  border-radius: var(--radius-full);
  transition: background-color var(--transition-fast) var(--transition-timing);
}

.top-header__user-menu:hover {
  background: var(--color-surface-alt);
}

.top-header__user-avatar {
  width: 32px;
  height: 32px;
  background: var(--color-primary);
  color: var(--color-text-primary);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

/* Page Content */
.page-content {
  flex: 1;
  padding: var(--space-6);
  overflow-y: auto;
}

/* Responsive Design */
@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform var(--transition-base) var(--transition-timing);
  }
  
  .sidebar--open {
    transform: translateX(0);
  }
  
  .main-content {
    margin-left: 0;
  }
  
  .page-content {
    padding: var(--space-4);
  }
  
  .top-header {
    padding: 0 var(--space-4);
  }
  
  .top-header__title {
    font-size: var(--font-size-lg);
  }
}

/* Tablet */
@media (min-width: 769px) and (max-width: 1024px) {
  .sidebar__logo-text {
    display: none;
  }
  
  .page-content {
    padding: var(--space-5);
  }
}
</style>
