/**
 * Test scaffolding for Subscription Component - SubscriptionForm
 *
 * @description Vue/Nuxt 3 SubscriptionForm component tests using Vitest and Vue Test Utils
 * @implNote This is a test scaffolding file. Full test implementation requires mentor-reviewer approval.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'

// Mock SubscriptionForm component
const SubscriptionForm = {
  name: 'SubscriptionForm',
  template: `
    <form data-testid="subscription-form" @submit.prevent="handleSubmit">
      <div>
        <label for="customerId">Customer</label>
        <select
          :disabled="disabled || loading"
          :value="subscription?.customerId || formData.customerId"
          @change="updateField('customerId', $event.target.value)"
          name="customerId"
          id="customerId"
        >
          <option value="">Select Customer</option>
          <option value="cust-001">John Doe</option>
          <option value="cust-002">Jane Smith</option>
        </select>
        <span v-if="errors.customerId" data-testid="error-customerId">{{ errors.customerId }}</span>
      </div>

      <div>
        <label for="planName">Plan Name</label>
        <input
          :readonly="mode === 'view'"
          :disabled="disabled || loading"
          :value="subscription?.planName || formData.planName"
          @input="updateField('planName', $event.target.value)"
          name="planName"
          id="planName"
          type="text"
          placeholder="Premium Plan"
        />
        <span v-if="errors.planName" data-testid="error-planName">{{ errors.planName }}</span>
      </div>

      <div>
        <label for="price">Price</label>
        <input
          :readonly="mode === 'view'"
          :disabled="disabled || loading"
          :value="subscription?.price || formData.price"
          @input="updateField('price', $event.target.value)"
          name="price"
          id="price"
          type="number"
          step="0.01"
          min="0"
        />
        <span v-if="errors.price" data-testid="error-price">{{ errors.price }}</span>
      </div>

      <div>
        <label for="currency">Currency</label>
        <select
          :disabled="disabled || loading || mode === 'view'"
          :value="subscription?.currency || formData.currency"
          @change="updateField('currency', $event.target.value)"
          name="currency"
          id="currency"
        >
          <option value="">Select Currency</option>
          <option value="USD">USD</option>
          <option value="EUR">EUR</option>
          <option value="GBP">GBP</option>
        </select>
        <span v-if="errors.currency" data-testid="error-currency">{{ errors.currency }}</span>
      </div>

      <div>
        <label for="billingCycle">Billing Cycle</label>
        <select
          :disabled="disabled || loading || mode === 'view'"
          :value="subscription?.billingCycle || formData.billingCycle"
          @change="updateField('billingCycle', $event.target.value)"
          name="billingCycle"
          id="billingCycle"
        >
          <option value="monthly">Monthly</option>
          <option value="quarterly">Quarterly</option>
          <option value="yearly">Yearly</option>
        </select>
        <span v-if="errors.billingCycle" data-testid="error-billingCycle">{{ errors.billingCycle }}</span>
      </div>

      <div>
        <label for="status">Status</label>
        <select
          :disabled="disabled || loading || mode === 'view'"
          :value="subscription?.status || formData.status"
          @change="updateField('status', $event.target.value)"
          name="status"
          id="status"
        >
          <option value="active">Active</option>
          <option value="cancelled">Cancelled</option>
          <option value="expired">Expired</option>
          <option value="suspended">Suspended</option>
        </select>
        <span v-if="errors.status" data-testid="error-status">{{ errors.status }}</span>
      </div>

      <div>
        <label for="startDate">Start Date</label>
        <input
          :readonly="mode === 'view'"
          :disabled="disabled || loading"
          :value="subscription?.startDate || formData.startDate"
          @input="updateField('startDate', $event.target.value)"
          name="startDate"
          id="startDate"
          type="date"
        />
        <span v-if="errors.startDate" data-testid="error-startDate">{{ errors.startDate }}</span>
      </div>

      <div>
        <label for="endDate">End Date</label>
        <input
          :readonly="mode === 'view'"
          :disabled="disabled || loading"
          :value="subscription?.endDate || formData.endDate"
          @input="updateField('endDate', $event.target.value)"
          name="endDate"
          id="endDate"
          type="date"
        />
        <span v-if="errors.endDate" data-testid="error-endDate">{{ errors.endDate }}</span>
      </div>

      <div v-if="showOptions">
        <label>
          <input
            type="checkbox"
            :checked="formData.autoRenew || subscription?.autoRenew"
            @change="updateField('autoRenew', $event.target.checked)"
            :disabled="disabled || loading || mode === 'view'"
          />
          Auto-Renew
        </label>
      </div>

      <div v-if="Object.keys(errors).length > 0" data-testid="error-summary">
        {{ Object.keys(errors).length }} validation errors
      </div>

      <div class="form-actions">
        <button data-testid="save-button" v-if="mode === 'create'">Save Subscription</button>
        <button data-testid="update-button" v-if="mode === 'edit'">Update Subscription</button>
        <button data-testid="edit-button" v-if="mode === 'view'">Edit</button>
        <button data-testid="cancel-button" type="button" @click="$emit('subscription-cancel')">Cancel</button>
      </div>

      <div v-if="loading" data-testid="loading">Loading...</div>
    </form>
  `,
  props: {
    subscription: Object,
    mode: {
      type: String,
      default: 'create',
      validator: (value) => ['create', 'edit', 'view'].includes(value)
    },
    showOptions: Boolean,
    disabled: Boolean,
    loading: Boolean
  },
  emits: ['submit', 'cancel'],
  data() {
    return {
      formData: {
        customerId: '',
        planName: '',
        price: '',
        currency: '',
        billingCycle: 'monthly',
        status: 'active',
        startDate: '',
        endDate: '',
        autoRenew: false
      },
      errors: {},
      isSubmitting: false
    }
  },
  methods: {
    updateField(field: string, value: any) {
      this.formData[field] = value
      if (this.errors[field]) {
        this.errors[field] = ''
      }
    },
    handleSubmit() {
      if (this.isSubmitting) return

      this.errors = {}
      this.isSubmitting = true

      if (!this.formData.customerId) {
        this.errors.customerId = 'Customer is required'
        this.isSubmitting = false
        return
      }

      if (!this.formData.planName) {
        this.errors.planName = 'Plan name is required'
        this.isSubmitting = false
        return
      }

      if (!this.formData.price) {
        this.errors.price = 'Price is required'
        this.isSubmitting = false
        return
      }

      if (!this.formData.currency) {
        this.errors.currency = 'Currency is required'
        this.isSubmitting = false
        return
      }

      if (!this.formData.billingCycle) {
        this.errors.billingCycle = 'Billing cycle is required'
        this.isSubmitting = false
        return
      }

      if (!this.formData.startDate) {
        this.errors.startDate = 'Start date is required'
        this.isSubmitting = false
        return
      }

      if (!this.formData.endDate) {
        this.errors.endDate = 'End date is required'
        this.isSubmitting = false
        return
      }

      this.$emit('submit', { ...this.formData })

      setTimeout(() => {
        this.isSubmitting = false
      }, 100)
    }
  }
}

// SubscriptionForm component props interface
interface SubscriptionFormProps {
  subscription?: {
    id?: string
    customerId: string
    planName: string
    price: number
    currency: string
    billingCycle: 'monthly' | 'quarterly' | 'yearly'
    status: 'active' | 'cancelled' | 'expired' | 'suspended'
    startDate: string
    endDate: string
    autoRenew?: boolean
  }
  mode?: 'create' | 'edit' | 'view'
  showOptions?: boolean
  disabled?: boolean
  loading?: boolean
}

// Mock subscription data
const mockSubscriptionData: SubscriptionFormProps['subscription'] = {
  id: 'sub-001',
  customerId: 'cust-001',
  planName: 'Premium Plan',
  price: 99.99,
  currency: 'USD',
  billingCycle: 'monthly',
  status: 'active',
  startDate: '2024-01-01',
  endDate: '2024-12-31',
  autoRenew: true
}

describe('Subscription Component - SubscriptionForm', () => {
  describe('Rendering', () => {
    it('should render with default props', () => {
      const wrapper = mount(SubscriptionForm, {
        global: {
          plugins: [createTestingPinia()]
        }
      })

      expect(wrapper.find('form').exists()).toBe(true)
      expect(wrapper.find('[data-testid="subscription-form"]').exists()).toBe(true)
    })

    it('should render all form fields', () => {
      const wrapper = mount(SubscriptionForm, {
        global: {
          plugins: [createTestingPinia()]
        }
      })

      expect(wrapper.find('[name="customerId"]').exists()).toBe(true)
      expect(wrapper.find('[name="planName"]').exists()).toBe(true)
      expect(wrapper.find('[name="price"]').exists()).toBe(true)
      expect(wrapper.find('[name="currency"]').exists()).toBe(true)
      expect(wrapper.find('[name="billingCycle"]').exists()).toBe(true)
      expect(wrapper.find('[name="status"]').exists()).toBe(true)
      expect(wrapper.find('[name="startDate"]').exists()).toBe(true)
      expect(wrapper.find('[name="endDate"]').exists()).toBe(true)
    })

    it('should pre-fill fields with subscription data in edit mode', () => {
      const wrapper = mount(SubscriptionForm, {
        props: {
          subscription: mockSubscriptionData,
          mode: 'edit'
        },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      expect(wrapper.find('[name="customerId"]').element.value).toBe(mockSubscriptionData.customerId)
      expect(wrapper.find('[name="planName"]').element.value).toBe(mockSubscriptionData.planName)
      expect(wrapper.find('[name="price"]').element.value).toBe(String(mockSubscriptionData.price))
      expect(wrapper.find('[name="currency"]').element.value).toBe(mockSubscriptionData.currency)
    })

    it('should render empty form in create mode', () => {
      const wrapper = mount(SubscriptionForm, {
        props: {
          mode: 'create'
        },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      expect(wrapper.find('[name="customerId"]').element.value).toBe('')
      expect(wrapper.find('[name="planName"]').element.value).toBe('')
      expect(wrapper.find('[name="price"]').element.value).toBe('')
    })

    it('should render read-only form in view mode', () => {
      const wrapper = mount(SubscriptionForm, {
        props: {
          subscription: mockSubscriptionData,
          mode: 'view'
        },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      expect(wrapper.find('[name="customerId"]').attributes('disabled')).toBeDefined()
      expect(wrapper.find('[name="planName"]').attributes('readonly')).toBeDefined()
      expect(wrapper.find('[name="price"]').attributes('readonly')).toBeDefined()
    })

    it('should show options checkbox when showOptions is true', () => {
      const wrapper = mount(SubscriptionForm, {
        props: {
          showOptions: true
        },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      expect(wrapper.find('input[type="checkbox"]').exists()).toBe(true)
      expect(wrapper.text()).toContain('Auto-Renew')
    })

    it('should hide options checkbox when showOptions is false', () => {
      const wrapper = mount(SubscriptionForm, {
        props: {
          showOptions: false
        },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      expect(wrapper.find('input[type="checkbox"]').exists()).toBe(false)
    })
  })

  describe('Form Modes', () => {
    it('should show save button in create mode', () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      expect(wrapper.find('[data-testid="save-button"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="update-button"]').exists()).toBe(false)
      expect(wrapper.find('[data-testid="edit-button"]').exists()).toBe(false)
    })

    it('should show update button in edit mode', () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'edit' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      expect(wrapper.find('[data-testid="update-button"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="save-button"]').exists()).toBe(false)
      expect(wrapper.find('[data-testid="edit-button"]').exists()).toBe(false)
    })

    it('should show edit button in view mode', () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'view' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      expect(wrapper.find('[data-testid="edit-button"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="save-button"]').exists()).toBe(false)
      expect(wrapper.find('[data-testid="update-button"]').exists()).toBe(false)
    })

    it('should disable fields in view mode', () => {
      const wrapper = mount(SubscriptionForm, {
        props: {
          subscription: mockSubscriptionData,
          mode: 'view'
        },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      expect(wrapper.find('[name="customerId"]').attributes('disabled')).toBeDefined()
      expect(wrapper.find('[name="planName"]').attributes('readonly')).toBeDefined()
    })

    it('should allow field editing in edit mode', () => {
      const wrapper = mount(SubscriptionForm, {
        props: {
          subscription: mockSubscriptionData,
          mode: 'edit'
        },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      expect(wrapper.find('[name="customerId"]').attributes('disabled')).toBeUndefined()
      expect(wrapper.find('[name="planName"]').attributes('readonly')).toBeUndefined()
    })
  })

  describe('Form Validation', () => {
    it('should validate required customer', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const customerSelect = wrapper.find('[name="customerId"]')
      await customerSelect.setValue('')
      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.find('[data-testid="error-customerId"]').text()).toBe('Customer is required')
    })

    it('should validate required plan name', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const planInput = wrapper.find('[name="planName"]')
      await planInput.setValue('')
      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.find('[data-testid="error-planName"]').text()).toBe('Plan name is required')
    })

    it('should validate required price', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const priceInput = wrapper.find('[name="price"]')
      await priceInput.setValue('')
      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.find('[data-testid="error-price"]').text()).toBe('Price is required')
    })

    it('should validate required currency', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const currencySelect = wrapper.find('[name="currency"]')
      await currencySelect.setValue('')
      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.find('[data-testid="error-currency"]').text()).toBe('Currency is required')
    })

    it('should validate required billing cycle', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const cycleSelect = wrapper.find('[name="billingCycle"]')
      await cycleSelect.setValue('')
      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.find('[data-testid="error-billingCycle"]').text()).toBe('Billing cycle is required')
    })

    it('should validate required start date', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const startDateInput = wrapper.find('[name="startDate"]')
      await startDateInput.setValue('')
      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.find('[data-testid="error-startDate"]').text()).toBe('Start date is required')
    })

    it('should validate required end date', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const endDateInput = wrapper.find('[name="endDate"]')
      await endDateInput.setValue('')
      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.find('[data-testid="error-endDate"]').text()).toBe('End date is required')
    })

    it('should prevent form submission with invalid data', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      await wrapper.find('[name="customerId"]').setValue('')
      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.emitted('submit')).toBeFalsy()
    })

    it('should show summary of validation errors', () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      wrapper.setData({
        errors: {
          customerId: 'Customer is required',
          planName: 'Plan name is required'
        }
      })

      expect(wrapper.find('[data-testid="error-summary"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="error-summary"]').text()).toContain('2 validation errors')
    })
  })

  describe('Form Submission', () => {
    it('should emit submit event when form is submitted', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      await wrapper.find('[name="customerId"]').setValue('cust-001')
      await wrapper.find('[name="planName"]').setValue('Premium Plan')
      await wrapper.find('[name="price"]').setValue('99.99')
      await wrapper.find('[name="currency"]').setValue('USD')
      await wrapper.find('[name="billingCycle"]').setValue('monthly')
      await wrapper.find('[name="startDate"]').setValue('2024-01-01')
      await wrapper.find('[name="endDate"]').setValue('2024-12-31')
      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.emitted('submit')).toBeTruthy()
    })

    it('should emit submit event with form data', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      await wrapper.find('[name="customerId"]').setValue('cust-002')
      await wrapper.find('[name="planName"]').setValue('Basic Plan')
      await wrapper.find('[name="price"]').setValue('49.99')
      await wrapper.find('[name="currency"]').setValue('USD')
      await wrapper.find('[name="billingCycle"]').setValue('monthly')
      await wrapper.find('[name="status"]').setValue('active')
      await wrapper.find('[name="startDate"]').setValue('2024-02-01')
      await wrapper.find('[name="endDate"]').setValue('2025-01-31')
      await wrapper.find('form').trigger('submit.prevent')

      const submitEvents = wrapper.emitted('submit') as any[]
      expect(submitEvents[0][0]).toMatchObject({
        customerId: 'cust-002',
        planName: 'Basic Plan',
        price: '49.99',
        currency: 'USD',
        billingCycle: 'monthly',
        status: 'active',
        startDate: '2024-02-01',
        endDate: '2025-01-31'
      })
    })

    it('should show loading state during submission', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      await wrapper.find('[name="customerId"]').setValue('cust-001')
      await wrapper.find('[name="planName"]').setValue('Premium Plan')
      await wrapper.find('[name="price"]').setValue('99.99')
      await wrapper.find('[name="currency"]').setValue('USD')
      await wrapper.find('[name="billingCycle"]').setValue('monthly')
      await wrapper.find('[name="startDate"]').setValue('2024-01-01')
      await wrapper.find('[name="endDate"]').setValue('2024-12-31')

      const submitPromise = wrapper.find('form').trigger('submit.prevent')
      expect(wrapper.find('[data-testid="loading"]').exists()).toBe(true)

      await submitPromise
      await flushPromises()
    })

    it('should prevent double submission', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      await wrapper.find('[name="customerId"]').setValue('cust-001')
      await wrapper.find('[name="planName"]').setValue('Premium Plan')
      await wrapper.find('[name="price"]').setValue('99.99')
      await wrapper.find('[name="currency"]').setValue('USD')
      await wrapper.find('[name="billingCycle"]').setValue('monthly')
      await wrapper.find('[name="startDate"]').setValue('2024-01-01')
      await wrapper.find('[name="endDate"]').setValue('2024-12-31')

      await wrapper.find('form').trigger('submit.prevent')
      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.emitted('submit')).toHaveLength(1)
    })
  })

  describe('Events', () => {
    it('should emit cancel event on cancel button click', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      await wrapper.find('[data-testid="cancel-button"]').trigger('click')

      expect(wrapper.emitted('cancel')).toBeTruthy()
    })

    it('should clear validation errors on field update', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { mode: 'create' },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      wrapper.setData({
        errors: {
          customerId: 'Customer is required'
        }
      })

      const customerSelect = wrapper.find('[name="customerId"]')
      await customerSelect.setValue('cust-001')

      expect(wrapper.find('[data-testid="error-customerId"]').exists()).toBe(false)
    })
  })

  describe('Field Updates', () => {
    it('should update customer field', async () => {
      const wrapper = mount(SubscriptionForm, {
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const customerSelect = wrapper.find('[name="customerId"]')
      await customerSelect.setValue('cust-002')

      expect(customerSelect.element.value).toBe('cust-002')
    })

    it('should update plan name field', async () => {
      const wrapper = mount(SubscriptionForm, {
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const planInput = wrapper.find('[name="planName"]')
      await planInput.setValue('Enterprise Plan')

      expect(planInput.element.value).toBe('Enterprise Plan')
    })

    it('should update price field', async () => {
      const wrapper = mount(SubscriptionForm, {
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const priceInput = wrapper.find('[name="price"]')
      await priceInput.setValue('299.99')

      expect(priceInput.element.value).toBe('299.99')
    })

    it('should update currency selection', async () => {
      const wrapper = mount(SubscriptionForm, {
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const currencySelect = wrapper.find('[name="currency"]')
      await currencySelect.setValue('EUR')

      expect(currencySelect.element.value).toBe('EUR')
    })

    it('should update billing cycle selection', async () => {
      const wrapper = mount(SubscriptionForm, {
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const cycleSelect = wrapper.find('[name="billingCycle"]')
      await cycleSelect.setValue('yearly')

      expect(cycleSelect.element.value).toBe('yearly')
    })

    it('should update status selection', async () => {
      const wrapper = mount(SubscriptionForm, {
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const statusSelect = wrapper.find('[name="status"]')
      await statusSelect.setValue('cancelled')

      expect(statusSelect.element.value).toBe('cancelled')
    })

    it('should update start date field', async () => {
      const wrapper = mount(SubscriptionForm, {
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const startDateInput = wrapper.find('[name="startDate"]')
      await startDateInput.setValue('2024-03-01')

      expect(startDateInput.element.value).toBe('2024-03-01')
    })

    it('should update end date field', async () => {
      const wrapper = mount(SubscriptionForm, {
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const endDateInput = wrapper.find('[name="endDate"]')
      await endDateInput.setValue('2024-12-31')

      expect(endDateInput.element.value).toBe('2024-12-31')
    })

    it('should update auto-renew checkbox', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: { showOptions: true },
        global: {
          plugins: [createTestingPinia()]
        }
      })

      const autoRenewCheckbox = wrapper.find('input[type="checkbox"]')
      await autoRenewCheckbox.setValue()

      expect(autoRenewCheckbox.element.checked).toBe(true)
    })
  })
})
