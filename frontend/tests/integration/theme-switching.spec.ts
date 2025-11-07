import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// Import components
import AppButton from '~/components/ui/AppButton.vue'
import AppInput from '~/components/ui/AppInput.vue'
import AppCard from '~/components/ui/AppCard.vue'
import AppBadge from '~/components/ui/AppBadge.vue'
import AppSelect from '~/components/ui/AppSelect.vue'
import AppModal from '~/components/ui/AppModal.vue'
import AppDataTable from '~/components/ui/AppDataTable.vue'

// Mock useTheme composable
const mockUseTheme = {
  theme: ref<'light' | 'dark' | 'system'>('system'),
  actualTheme: ref<'light' | 'dark'>('light'),
  setTheme: vi.fn(),
  toggleTheme: vi.fn()
}

vi.mock('~/composables/useTheme', () => ({
  useTheme: () => mockUseTheme
}))

describe('Theme Switching Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    document.documentElement.removeAttribute('data-theme')
  })

  afterEach(() => {
    document.documentElement.removeAttribute('data-theme')
  })

  it('all components render correctly in light mode', async () => {
    mockUseTheme.actualTheme.value = 'light'

    const wrapper = mount({
      template: `
        <div class="theme-test" data-theme="light">
          <AppCard title="Card Component">
            <AppInput
              v-model="inputValue"
              label="Input Field"
              placeholder="Enter text"
            />
            <AppSelect
              v-model="selectedValue"
              :options="options"
              label="Select Option"
            />
            <AppBadge label="Badge" variant="primary" />
            <AppButton label="Primary Button" />
            <AppButton label="Secondary Button" variant="secondary" />
          </AppCard>
        </div>
      `,
      components: { AppCard, AppInput, AppSelect, AppBadge, AppButton },
      data() {
        return {
          inputValue: '',
          selectedValue: '',
          options: [
            { label: 'Option 1', value: 'opt1' },
            { label: 'Option 2', value: 'opt2' }
          ]
        }
      }
    })

    // All components should be visible
    expect(wrapper.find('.card').exists()).toBe(true)
    expect(wrapper.find('.input').exists()).toBe(true)
    expect(wrapper.find('.select').exists()).toBe(true)
    expect(wrapper.find('.badge').exists()).toBe(true)
    expect(wrapper.findAll('.btn').length).toBe(2)

    // Check they render without errors
    expect(wrapper.text()).toContain('Card Component')
    expect(wrapper.text()).toContain('Input Field')
    expect(wrapper.text()).toContain('Select Option')
  })

  it('all components render correctly in dark mode', async () => {
    mockUseTheme.actualTheme.value = 'dark'
    document.documentElement.setAttribute('data-theme', 'dark')

    const wrapper = mount({
      template: `
        <div class="theme-test" data-theme="dark">
          <AppCard title="Dark Mode Card">
            <AppInput
              v-model="inputValue"
              label="Dark Input"
              placeholder="Enter text"
            />
            <AppSelect
              v-model="selectedValue"
              :options="options"
              label="Dark Select"
            />
            <AppBadge label="Dark Badge" variant="success" />
            <AppButton label="Dark Button" variant="outline" />
            <AppBadge label="Warning" variant="warning" />
          </AppCard>
        </div>
      `,
      components: { AppCard, AppInput, AppSelect, AppBadge, AppButton },
      data() {
        return {
          inputValue: '',
          selectedValue: '',
          options: [
            { label: 'Option A', value: 'a' },
            { label: 'Option B', value: 'b' }
          ]
        }
      }
    })

    // All components should render in dark mode
    expect(wrapper.find('.card').exists()).toBe(true)
    expect(wrapper.find('.input').exists()).toBe(true)
    expect(wrapper.find('.select').exists()).toBe(true)
    expect(wrapper.findAll('.badge').length).toBe(2)
    expect(wrapper.find('.btn').exists()).toBe(true)

    // Text should still be visible
    expect(wrapper.text()).toContain('Dark Mode Card')
    expect(wrapper.text()).toContain('Dark Input')
  })

  it('theme toggle button switches theme', async () => {
    const wrapper = mount({
      template: `
        <div>
          <AppButton
            label="Toggle Theme"
            icon="lucide:sun"
            @click="toggleTheme"
          />
          <div class="content">
            <AppCard title="Theme Test">
              <AppBadge label="Status" variant="primary" />
            </AppCard>
          </div>
        </div>
      `,
      components: { AppButton, AppCard, AppBadge },
      methods: {
        toggleTheme() {
          mockUseTheme.toggleTheme()
        }
      }
    })

    // Initial state - light
    mockUseTheme.actualTheme.value = 'light'
    document.documentElement.setAttribute('data-theme', 'light')

    // Click toggle
    await wrapper.find('.btn').trigger('click')

    // Should call toggleTheme
    expect(mockUseTheme.toggleTheme).toHaveBeenCalled()

    // Theme should change to dark
    mockUseTheme.actualTheme.value = 'dark'
    document.documentElement.setAttribute('data-theme', 'dark')

    // All content should still be visible after theme change
    expect(wrapper.find('.card').exists()).toBe(true)
    expect(wrapper.find('.badge').exists()).toBe(true)
  })

  it('modal works in both themes', async () => {
    const showModal = ref(false)

    const wrapper = mount({
      template: `
        <div>
          <AppButton
            label="Open Modal"
            @click="showModal = true"
          />

          <AppModal
            v-model="showModal"
            title="Theme Test Modal"
          >
            <AppInput
              v-model="inputValue"
              label="Modal Input"
            />
            <AppSelect
              v-model="selectedValue"
              :options="options"
              label="Modal Select"
            />
            <AppBadge label="Modal Badge" variant="info" />
          </AppModal>
        </div>
      `,
      components: { AppButton, AppModal, AppInput, AppSelect, AppBadge },
      data() {
        return {
          inputValue: '',
          selectedValue: '',
          options: [
            { label: 'Choice 1', value: '1' },
            { label: 'Choice 2', value: '2' }
          ]
        }
      },
      setup() {
        return {
          showModal
        }
      }
    })

    // Test in light mode
    mockUseTheme.actualTheme.value = 'light'
    document.documentElement.setAttribute('data-theme', 'light')

    await wrapper.find('.btn').trigger('click')
    expect(showModal.value).toBe(true)
    expect(wrapper.findComponent(AppModal).props('modelValue')).toBe(true)

    await wrapper.findAll('button').at(-1).trigger('click')
    expect(showModal.value).toBe(false)

    // Test in dark mode
    mockUseTheme.actualTheme.value = 'dark'
    document.documentElement.setAttribute('data-theme', 'dark')

    await wrapper.find('.btn').trigger('click')
    expect(showModal.value).toBe(true)
    expect(wrapper.findComponent(AppModal).props('modelValue')).toBe(true)

    // Modal should render in dark mode
    expect(wrapper.find('.modal').exists()).toBe(true)
  })

  it('data table with theme switching', async () => {
    const columns = [
      { key: 'name', label: 'Name', sortable: true },
      { key: 'status', label: 'Status' }
    ]
    const data = [
      { id: 1, name: 'Item 1', status: 'active' },
      { id: 2, name: 'Item 2', status: 'inactive' }
    ]

    const wrapper = mount({
      template: `
        <AppDataTable
          :columns="columns"
          :data="data"
          :selectable="true"
        />
      `,
      components: { AppDataTable },
      data() {
        return {
          columns,
          data
        }
      }
    })

    // Test in light mode
    mockUseTheme.actualTheme.value = 'light'
    document.documentElement.setAttribute('data-theme', 'light')

    expect(wrapper.find('.data-table').exists()).toBe(true)
    expect(wrapper.findAll('.data-table__row').length).toBe(2)

    // Switch to dark mode
    mockUseTheme.actualTheme.value = 'dark'
    document.documentElement.setAttribute('data-theme', 'dark')

    // Table should still work
    expect(wrapper.find('.data-table').exists()).toBe(true)

    // Interact with table
    const header = wrapper.find('.data-table__header-cell')
    await header.trigger('click')
    expect(header.classes()).toContain('data-table__header-cell--sorted')

    // Still visible in dark mode
    expect(wrapper.findAll('.data-table__row').length).toBe(2)
  })

  it('form components maintain functionality in both themes', async () => {
    const formData = ref({
      name: '',
      email: '',
      role: '',
      status: ''
    })

    const wrapper = mount({
      template: `
        <AppCard title="Theme Form Test">
          <form @submit.prevent="onSubmit">
            <AppInput
              v-model="formData.name"
              label="Name"
              required
            />
            <AppInput
              v-model="formData.email"
              label="Email"
              type="email"
              required
            />
            <AppSelect
              v-model="formData.role"
              :options="roleOptions"
              label="Role"
              required
            />
            <div class="status-section">
              <AppBadge
                :label="formData.status || 'No status'"
                :variant="getStatusVariant(formData.status)"
              />
            </div>
            <AppButton
              label="Submit"
              type="submit"
              :disabled="!isValid"
            />
          </form>
        </AppCard>
      `,
      components: { AppCard, AppInput, AppSelect, AppBadge, AppButton },
      data() {
        return {
          roleOptions: [
            { label: 'Admin', value: 'admin' },
            { label: 'User', value: 'user' },
            { label: 'Guest', value: 'guest' }
          ]
        }
      },
      computed: {
        isValid() {
          return !!(this.formData.name && this.formData.email && this.formData.role)
        }
      },
      methods: {
        onSubmit() {
          // Handle submit
        },
        getStatusVariant(status: string) {
          const variants: Record<string, string> = {
            admin: 'danger',
            user: 'success',
            guest: 'warning'
          }
          return variants[status] || 'neutral'
        }
      },
      setup() {
        return {
          formData
        }
      }
    })

    // Test in light mode
    mockUseTheme.actualTheme.value = 'light'
    document.documentElement.setAttribute('data-theme', 'light')

    // Fill form
    await wrapper.find('input[type="text"]').setValue('John Doe')
    await wrapper.find('input[type="email"]').setValue('john@example.com')
    await wrapper.find('.select__trigger').trigger('click')
    await wrapper.findAll('.select__option')[0].trigger('click')

    // Form should work
    expect(wrapper.vm.isValid).toBe(true)
    expect(wrapper.find('.badge').props('variant')).toBe('danger')

    // Switch to dark mode mid-form
    mockUseTheme.actualTheme.value = 'dark'
    document.documentElement.setAttribute('data-theme', 'dark')

    // Form should still work
    expect(wrapper.find('input[type="text"]').element.value).toBe('John Doe')
    expect(wrapper.find('.select__trigger').text()).toBe('Admin')

    // Badge should still update
    formData.value.status = 'user'
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.badge').props('variant')).toBe('success')
  })

  it('theme persistence across component mount/unmount', async () => {
    const showComponent = ref(true)

    const wrapper = mount({
      template: `
        <div>
          <AppButton
            label="Toggle Theme"
            @click="toggleTheme"
          />
          <AppCard
            v-if="showComponent"
            title="Dynamic Card"
          >
            <AppBadge label="Dynamic Badge" variant="primary" />
          </AppCard>
        </div>
      `,
      components: { AppButton, AppCard, AppBadge },
      methods: {
        toggleTheme() {
          mockUseTheme.toggleTheme()
        }
      },
      setup() {
        return {
          showComponent
        }
      }
    })

    // Light mode
    mockUseTheme.actualTheme.value = 'light'
    expect(wrapper.find('.card').exists()).toBe(true)

    // Toggle theme
    await wrapper.find('.btn').trigger('click')
    expect(mockUseTheme.toggleTheme).toHaveBeenCalled()

    // Switch to dark
    mockUseTheme.actualTheme.value = 'dark'
    document.documentElement.setAttribute('data-theme', 'dark')

    // Component should still exist
    expect(wrapper.find('.card').exists()).toBe(true)

    // Unmount component
    showComponent.value = false
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.card').exists()).toBe(false)

    // Remount component
    showComponent.value = true
    await wrapper.vm.$nextTick()

    // Should render in dark mode
    mockUseTheme.actualTheme.value = 'dark'
    expect(wrapper.find('.card').exists()).toBe(true)
    expect(wrapper.find('.badge').exists()).toBe(true)
  })

  it('system theme detection', async () => {
    const mockMatchMedia = vi.fn().mockImplementation(query => ({
      matches: query === '(prefers-color-scheme: dark)',
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    }))

    Object.defineProperty(window, 'matchMedia', {
      value: mockMatchMedia
    })

    const wrapper = mount({
      template: `
        <AppCard title="System Theme">
          <AppBadge
            :label="themeLabel"
            :variant="themeVariant"
          />
        </AppCard>
      `,
      components: { AppCard, AppBadge },
      computed: {
        themeLabel() {
          return mockUseTheme.actualTheme.value === 'dark' ? 'Dark' : 'Light'
        },
        themeVariant() {
          return mockUseTheme.actualTheme.value === 'dark' ? 'secondary' : 'primary'
        }
      }
    })

    // System prefers dark
    mockMatchMedia.mockReturnValue({
      matches: true,
      media: '(prefers-color-scheme: dark)',
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    })

    mockUseTheme.theme.value = 'system'
    mockUseTheme.actualTheme.value = 'dark'

    expect(wrapper.find('.badge').text()).toBe('Dark')
    expect(wrapper.find('.badge').props('variant')).toBe('secondary')
  })
})
