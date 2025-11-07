import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// Import all components
import AppInput from '~/components/ui/AppInput.vue'
import AppSelect from '~/components/ui/AppSelect.vue'
import AppButton from '~/components/ui/AppButton.vue'
import AppModal from '~/components/ui/AppModal.vue'
import AppCard from '~/components/ui/AppCard.vue'
import AppBadge from '~/components/ui/AppBadge.vue'

describe('Customer Form Workflow Integration', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('complete customer creation flow', async () => {
    const mockSubmit = vi.fn()
    const mockCancel = vi.fn()

    const wrapper = mount({
      template: `
        <AppCard title="Create Customer" class="customer-form-card">
          <form @submit.prevent="onSubmit">
            <AppInput
              v-model="formData.name"
              label="Full Name"
              placeholder="Enter your name"
              required
              :error="errors.name"
            />
            <AppInput
              v-model="formData.email"
              label="Email"
              type="email"
              required
              :error="errors.email"
            />
            <AppSelect
              v-model="formData.country"
              :options="countryOptions"
              label="Country"
              placeholder="Select country"
              required
              :error="errors.country"
            />
            <AppInput
              v-model="formData.phone"
              label="Phone"
              type="tel"
              placeholder="+1234567890"
            />
            <div class="form-actions">
              <AppButton
                label="Cancel"
                variant="outline"
                @click="onCancel"
              />
              <AppButton
                label="Create Customer"
                type="submit"
                :loading="loading"
                :disabled="!isValid"
              />
            </div>
          </form>
        </AppCard>
      `,
      components: {
        AppCard,
        AppInput,
        AppSelect,
        AppButton
      },
      data() {
        return {
          formData: {
            name: '',
            email: '',
            country: '',
            phone: ''
          },
          errors: {} as Record<string, string>,
          loading: false,
          countryOptions: [
            { label: 'United States', value: 'us' },
            { label: 'Canada', value: 'ca' },
            { label: 'United Kingdom', value: 'uk' }
          ]
        }
      },
      computed: {
        isValid() {
          return !!(this.formData.name && this.formData.email && this.formData.country)
        }
      },
      methods: {
        async onSubmit() {
          this.loading = true
          await new Promise(resolve => setTimeout(resolve, 500))
          mockSubmit(this.formData)
          this.loading = false
        },
        onCancel() {
          mockCancel()
        }
      }
    })

    // Initial state
    expect(wrapper.find('.customer-form-card').exists()).toBe(true)
    expect(wrapper.findComponent(AppInput).exists()).toBe(true)
    expect(wrapper.findComponent(AppSelect).exists()).toBe(true)
    expect(wrapper.findComponent(AppButton).exists()).toBe(true)

    // Fill form
    await wrapper.find('input[type="text"]').setValue('John Doe')
    await wrapper.find('input[type="email"]').setValue('john@example.com')
    await wrapper.find('.select__trigger').trigger('click')
    await wrapper.findAll('.select__option')[0].trigger('click')
    await wrapper.find('input[type="tel"]').setValue('+1234567890')

    // Verify form data
    expect(wrapper.vm.formData.name).toBe('John Doe')
    expect(wrapper.vm.formData.email).toBe('john@example.com')
    expect(wrapper.vm.formData.country).toBe('us')
    expect(wrapper.vm.isValid).toBe(true)

    // Submit form
    await wrapper.find('button[type="submit"]').trigger('click')
    expect(mockSubmit).toHaveBeenCalledWith({
      name: 'John Doe',
      email: 'john@example.com',
      country: 'us',
      phone: '+1234567890'
    })
  })

  it('form validation errors are displayed', async () => {
    const wrapper = mount({
      template: `
        <AppCard title="Create Customer">
          <form>
            <AppInput
              v-model="name"
              label="Full Name"
              required
              :error="errors.name"
            />
            <AppInput
              v-model="email"
              label="Email"
              type="email"
              required
              :error="errors.email"
            />
          </form>
        </AppCard>
      `,
      components: { AppCard, AppInput },
      data() {
        return {
          name: '',
          email: 'invalid-email',
          errors: {
            name: 'Name is required',
            email: 'Invalid email format'
          }
        }
      }
    })

    // Check error messages are displayed
    const nameError = wrapper.find('.input__error')
    const emailError = wrapper.findAll('.input__error')[1]
    expect(nameError.text()).toBe('Name is required')
    expect(emailError.text()).toBe('Invalid email format')

    // Check error styling
    const nameInput = wrapper.find('input')
    expect(nameInput.classes()).toContain('input__field--error')
  })

  it('form with clearable fields', async () => {
    const wrapper = mount({
      template: `
        <AppCard title="Create Customer">
          <AppInput
            v-model="email"
            label="Email"
            type="email"
            clearable
          />
        </AppCard>
      `,
      components: { AppCard, AppInput },
      data() {
        return {
          email: 'test@example.com'
        }
      }
    })

    // Clear button should be visible
    const clearButton = wrapper.find('.input__clear')
    expect(clearButton.exists()).toBe(true)

    // Clear the field
    await clearButton.trigger('click')
    expect(wrapper.vm.email).toBe('')
  })

  it('form with password toggle', async () => {
    const wrapper = mount({
      template: `
        <AppCard title="Create Customer">
          <AppInput
            v-model="password"
            label="Password"
            type="password"
          />
        </AppCard>
      `,
      components: { AppCard, AppInput },
      data() {
        return {
          password: 'secret123'
        }
      }
    })

    // Password toggle button should exist
    const toggleButton = wrapper.find('.input__toggle')
    expect(toggleButton.exists()).toBe(true)

    // Toggle to show password
    await toggleButton.trigger('click')
    const passwordInput = wrapper.find('input')
    expect(passwordInput.attributes('type')).toBe('text')
  })

  it('form with searchable select', async () => {
    const wrapper = mount({
      template: `
        <AppCard title="Create Customer">
          <AppSelect
            v-model="selectedCountry"
            :options="countryOptions"
            label="Country"
            placeholder="Search country"
            searchable
          />
        </AppCard>
      `,
      components: { AppCard, AppSelect },
      data() {
        return {
          selectedCountry: '',
          countryOptions: [
            { label: 'United States', value: 'us' },
            { label: 'Canada', value: 'ca' },
            { label: 'Mexico', value: 'mx' }
          ]
        }
      }
    })

    // Open select
    await wrapper.find('.select__trigger').trigger('click')
    expect(wrapper.find('.select__dropdown--open').exists()).toBe(true)

    // Search for a country
    await wrapper.find('.select__search').setValue('canada')
    await wrapper.vm.$nextTick()

    // Only matching option should be visible
    const options = wrapper.findAll('.select__option')
    expect(options.length).toBe(1)
    expect(options[0].text()).toBe('Canada')

    // Select the option
    await options[0].trigger('click')
    expect(wrapper.vm.selectedCountry).toBe('ca')
  })

  it('form with disabled state', async () => {
    const wrapper = mount({
      template: `
        <AppCard title="Create Customer">
          <AppInput
            v-model="name"
            label="Name"
            :disabled="disabled"
          />
          <AppButton
            label="Submit"
            :disabled="disabled"
          />
        </AppCard>
      `,
      components: { AppCard, AppInput, AppButton },
      data() {
        return {
          name: 'John Doe',
          disabled: true
        }
      }
    })

    // Input should be disabled
    const input = wrapper.find('input')
    expect(input.attributes('disabled')).toBeDefined()

    // Button should be disabled
    const button = wrapper.find('button')
    expect(button.classes()).toContain('btn--disabled')
  })

  it('form with multiple selection', async () => {
    const wrapper = mount({
      template: `
        <AppCard title="Customer Preferences">
          <AppSelect
            v-model="selectedInterests"
            :options="interests"
            label="Interests"
            multiple
            placeholder="Select interests"
          />
        </AppCard>
      `,
      components: { AppCard, AppSelect },
      data() {
        return {
          selectedInterests: [],
          interests: [
            { label: 'Technology', value: 'tech' },
            { label: 'Sports', value: 'sports' },
            { label: 'Music', value: 'music' }
          ]
        }
      }
    })

    // Open select
    await wrapper.find('.select__trigger').trigger('click')

    // Select multiple options
    await wrapper.findAll('.select__option')[0].trigger('click')
    await wrapper.findAll('.select__option')[2].trigger('click')

    expect(wrapper.vm.selectedInterests).toEqual(['tech', 'music'])

    // Tags should be displayed
    const tags = wrapper.findAll('.select__tag')
    expect(tags.length).toBe(2)
  })

  it('form with confirmation modal', async () => {
    const showModal = ref(false)
    const formSubmitted = ref(false)

    const wrapper = mount({
      template: `
        <div>
          <AppCard title="Create Customer">
            <form @submit.prevent="onSubmit">
              <AppInput
                v-model="name"
                label="Name"
                required
              />
              <AppButton label="Submit" type="submit" />
            </form>
          </AppCard>

          <AppModal
            v-model="showModal"
            title="Confirm Submission"
          >
            <p>Are you sure you want to create this customer?</p>
            <template #footer>
              <AppButton
                label="Cancel"
                variant="outline"
                @click="showModal = false"
              />
              <AppButton
                label="Confirm"
                @click="confirmSubmission"
              />
            </template>
          </AppModal>
        </div>
      `,
      components: { AppCard, AppInput, AppButton, AppModal },
      data() {
        return {
          name: 'John Doe'
        }
      },
      setup() {
        return {
          showModal,
          formSubmitted,
          onSubmit() {
            showModal.value = true
          },
          confirmSubmission() {
            formSubmitted.value = true
            showModal.value = false
          }
        }
      }
    })

    // Submit form
    await wrapper.find('button[type="submit"]').trigger('click')

    // Modal should open
    expect(wrapper.findComponent(AppModal).props('modelValue')).toBe(true)

    // Confirm submission
    await wrapper.findAll('button').at(-1).trigger('click')

    // Modal should close and form should be submitted
    expect(showModal.value).toBe(false)
    expect(formSubmitted.value).toBe(true)
  })

  it('form in dark mode', async () => {
    document.documentElement.setAttribute('data-theme', 'dark')

    const wrapper = mount({
      template: `
        <AppCard title="Create Customer">
          <AppInput
            v-model="name"
            label="Name"
            placeholder="Enter name"
          />
          <AppSelect
            v-model="country"
            :options="countries"
            label="Country"
          />
          <AppButton label="Submit" />
        </AppCard>
      `,
      components: { AppCard, AppInput, AppSelect, AppButton },
      data() {
        return {
          name: 'John Doe',
          country: '',
          countries: [
            { label: 'US', value: 'us' },
            { label: 'CA', value: 'ca' }
          ]
        }
      }
    })

    // All components should render in dark mode
    expect(wrapper.find('.card').exists()).toBe(true)
    expect(wrapper.find('.input__field').exists()).toBe(true)
    expect(wrapper.find('.select__trigger').exists()).toBe(true)
    expect(wrapper.find('.btn').exists()).toBe(true)

    document.documentElement.removeAttribute('data-theme')
  })
})
