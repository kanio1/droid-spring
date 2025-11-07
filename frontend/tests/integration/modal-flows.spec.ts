import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// Import components
import AppModal from '~/components/ui/AppModal.vue'
import AppButton from '~/components/ui/AppButton.vue'
import AppInput from '~/components/ui/AppInput.vue'
import AppSelect from '~/components/ui/AppSelect.vue'

// Mock scroll lock
const mockScrollLock = vi.fn()
vi.mock('~/utils/scrollLock', () => ({
  lockScroll: mockScrollLock,
  unlockScroll: vi.fn()
}))

describe('Modal Flows Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('create customer modal flow', async () => {
    const showModal = ref(false)
    const customerData = ref<any>(null)

    const wrapper = mount({
      template: `
        <div>
          <AppButton
            label="Create Customer"
            @click="showModal = true"
          />

          <AppModal
            v-model="showModal"
            title="Create New Customer"
            size="lg"
            :close-on-backdrop="false"
          >
            <form @submit.prevent="onSubmit">
              <AppInput
                v-model="formData.name"
                label="Full Name"
                required
              />
              <AppInput
                v-model="formData.email"
                label="Email"
                type="email"
                required
              />
              <AppSelect
                v-model="formData.status"
                :options="statusOptions"
                label="Status"
                required
              />
            </form>
            <template #footer>
              <AppButton
                label="Cancel"
                variant="outline"
                @click="showModal = false"
              />
              <AppButton
                label="Create"
                @click="onSubmit"
              />
            </template>
          </AppModal>
        </div>
      `,
      components: { AppModal, AppButton, AppInput, AppSelect },
      data() {
        return {
          formData: {
            name: '',
            email: '',
            status: ''
          },
          statusOptions: [
            { label: 'Active', value: 'active' },
            { label: 'Inactive', value: 'inactive' }
          ]
        }
      },
      setup() {
        return {
          showModal,
          customerData,
          onSubmit() {
            customerData.value = { ...wrapper.vm.formData }
            showModal.value = false
          }
        }
      }
    })

    // Open modal
    await wrapper.find('.btn--primary').trigger('click')
    expect(showModal.value).toBe(true)
    expect(wrapper.findComponent(AppModal).props('modelValue')).toBe(true)

    // Fill form
    await wrapper.find('input[type="text"]').setValue('John Doe')
    await wrapper.find('input[type="email"]').setValue('john@example.com')
    await wrapper.find('.select__trigger').trigger('click')
    await wrapper.findAll('.select__option')[0].trigger('click')

    // Submit
    await wrapper.findAll('button').at(-1).trigger('click')

    // Modal closes and data is saved
    expect(showModal.value).toBe(false)
    expect(customerData.value).toEqual({
      name: 'John Doe',
      email: 'john@example.com',
      status: 'active'
    })
  })

  it('delete confirmation modal', async () => {
    const showDeleteModal = ref(false)
    const itemToDelete = ref<any>(null)
    const deleted = ref(false)

    const wrapper = mount({
      template: `
        <div>
          <AppButton
            label="Delete Item"
            variant="danger"
            @click="confirmDelete"
          />

          <AppModal
            v-model="showDeleteModal"
            title="Confirm Deletion"
            variant="alert"
            :persistent="true"
          >
            <p>Are you sure you want to delete this item?</p>
            <p v-if="itemToDelete"><strong>{{ itemToDelete.name }}</strong></p>
            <template #footer>
              <AppButton
                label="Cancel"
                variant="outline"
                @click="cancelDelete"
              />
              <AppButton
                label="Delete"
                variant="danger"
                @click="performDelete"
              />
            </template>
          </AppModal>
        </div>
      `,
      components: { AppModal, AppButton },
      data() {
        return {
          item: { id: 1, name: 'Test Item' }
        }
      },
      setup() {
        return {
          showDeleteModal,
          itemToDelete,
          deleted,
          confirmDelete() {
            itemToDelete.value = wrapper.vm.item
            showDeleteModal.value = true
          },
          cancelDelete() {
            showDeleteModal.value = false
            itemToDelete.value = null
          },
          performDelete() {
            deleted.value = true
            showDeleteModal.value = false
            itemToDelete.value = null
          }
        }
      }
    })

    // Trigger delete
    await wrapper.find('.btn--danger').trigger('click')

    // Modal should open
    expect(showDeleteModal.value).toBe(true)
    expect(wrapper.text()).toContain('Are you sure')

    // Cancel
    await wrapper.findAll('button')[0].trigger('click')
    expect(showDeleteModal.value).toBe(false)
    expect(deleted.value).toBe(false)

    // Try again and confirm
    await wrapper.find('.btn--danger').trigger('click')
    await wrapper.findAll('button').at(-1).trigger('click')

    expect(deleted.value).toBe(true)
  })

  it('modal with form validation', async () => {
    const showModal = ref(false)
    const errors = ref<Record<string, string>>({})

    const wrapper = mount({
      template: `
        <AppModal
          v-model="showModal"
          title="Login"
        >
          <form>
            <AppInput
              v-model="email"
              label="Email"
              type="email"
              required
              :error="errors.email"
            />
            <AppInput
              v-model="password"
              label="Password"
              type="password"
              required
              :error="errors.password"
            />
          </form>
          <template #footer>
            <AppButton
              label="Login"
              @click="onLogin"
            />
          </template>
        </AppModal>
      `,
      components: { AppModal, AppInput, AppButton },
      data() {
        return {
          email: '',
          password: ''
        }
      },
      setup() {
        return {
          showModal,
          errors,
          onLogin() {
            errors.value = {}
            if (!wrapper.vm.email) {
              errors.value.email = 'Email is required'
            }
            if (!wrapper.vm.password) {
              errors.value.password = 'Password is required'
            }
            if (Object.keys(errors.value).length === 0) {
              showModal.value = false
            }
          }
        }
      }
    })

    // Open modal
    showModal.value = true
    await wrapper.vm.$nextTick()

    // Try to submit empty form
    await wrapper.find('.btn--primary').trigger('click')

    // Errors should be shown
    expect(errors.value.email).toBe('Email is required')
    expect(errors.value.password).toBe('Password is required')

    // Fill form
    await wrapper.find('input[type="email"]').setValue('test@example.com')
    await wrapper.find('input[type="password"]').setValue('password123')

    // Submit again
    await wrapper.find('.btn--primary').trigger('click')

    // Modal should close
    expect(showModal.value).toBe(false)
  })

  it('modal with searchable content', async () => {
    const showModal = ref(false)
    const searchTerm = ref('')
    const selectedItems = ref<any[]>([])

    const allItems = [
      { id: 1, name: 'Item 1' },
      { id: 2, name: 'Item 2' },
      { id: 3, name: 'Item 3' },
      { id: 4, name: 'Item 4' }
    ]

    const wrapper = mount({
      template: `
        <AppModal
          v-model="showModal"
          title="Select Items"
          size="lg"
        >
          <AppInput
            v-model="searchTerm"
            placeholder="Search items..."
            icon="lucide:search"
          />
          <div class="item-list">
            <div
              v-for="item in filteredItems"
              :key="item.id"
              class="item"
              @click="toggleItem(item)"
            >
              {{ item.name }}
            </div>
          </div>
          <template #footer>
            <AppButton
              label="Cancel"
              variant="outline"
              @click="showModal = false"
            />
            <AppButton
              label="Select"
              @click="onSelect"
            />
          </template>
        </AppModal>
      `,
      components: { AppModal, AppInput, AppButton },
      setup() {
        return {
          showModal,
          searchTerm,
          selectedItems,
          filteredItems: computed(() => {
            if (!searchTerm.value) return allItems
            return allItems.filter(item =>
              item.name.toLowerCase().includes(searchTerm.value.toLowerCase())
            )
          }),
          toggleItem(item: any) {
            const index = selectedItems.value.findIndex(i => i.id === item.id)
            if (index > -1) {
              selectedItems.value.splice(index, 1)
            } else {
              selectedItems.value.push(item)
            }
          },
          onSelect() {
            showModal.value = false
          }
        }
      }
    })

    // Open modal
    showModal.value = true
    await wrapper.vm.$nextTick()

    // Search for items
    await wrapper.find('input').setValue('Item 1')
    await wrapper.vm.$nextTick()

    expect(wrapper.findAll('.item').length).toBe(1)

    // Select item
    await wrapper.find('.item').trigger('click')

    // Clear search
    await wrapper.find('input').setValue('')
    await wrapper.vm.$nextTick()

    // All items should be visible again
    expect(wrapper.findAll('.item').length).toBe(4)

    // Confirm selection
    await wrapper.findAll('button').at(-1).trigger('click')
    expect(showModal.value).toBe(false)
    expect(selectedItems.value.length).toBe(1)
  })

  it('nested modal flow', async () => {
    const showFirstModal = ref(false)
    const showSecondModal = ref(false)

    const wrapper = mount({
      template: `
        <div>
          <AppButton
            label="Open First Modal"
            @click="showFirstModal = true"
          />

          <AppModal
            v-model="showFirstModal"
            title="First Modal"
          >
            <p>This is the first modal</p>
            <AppButton
              label="Open Second Modal"
              @click="showSecondModal = true"
            />
          </AppModal>

          <AppModal
            v-model="showSecondModal"
            title="Second Modal"
          >
            <p>This is the second modal</p>
            <AppButton
              label="Close"
              @click="showSecondModal = false"
            />
          </AppModal>
        </div>
      `,
      components: { AppModal, AppButton },
      setup() {
        return {
          showFirstModal,
          showSecondModal
        }
      }
    })

    // Open first modal
    await wrapper.find('.btn--primary').trigger('click')
    expect(showFirstModal.value).toBe(true)

    // Open second modal
    await wrapper.findAll('.btn--primary')[0].trigger('click')
    expect(showSecondModal.value).toBe(true)

    // Close second modal
    await wrapper.findAll('.btn--primary').at(-1).trigger('click')
    expect(showSecondModal.value).toBe(false)
    expect(showFirstModal.value).toBe(true)

    // Close first modal
    // Note: In real app, would need to handle nested modal backdrop clicks
  })

  it('modal with keyboard navigation', async () => {
    const showModal = ref(false)
    const focusedIndex = ref(0)

    const wrapper = mount({
      template: `
        <AppModal
          v-model="showModal"
          title="Keyboard Navigation"
        >
          <button ref="button1" @focus="focusedIndex = 0">Button 1</button>
          <button ref="button2" @focus="focusedIndex = 1">Button 2</button>
          <button ref="button3" @focus="focusedIndex = 2">Button 3</button>
        </AppModal>
      `,
      components: { AppModal },
      setup() {
        return {
          showModal,
          focusedIndex
        }
      }
    })

    // Open modal
    showModal.value = true
    await wrapper.vm.$nextTick()

    // Simulate tab navigation
    const modal = wrapper.findComponent(AppModal)
    await modal.trigger('keydown', { key: 'Tab' })
    await modal.trigger('keydown', { key: 'Tab' })

    // Close with Escape
    await modal.trigger('keydown', { key: 'Escape' })
    expect(showModal.value).toBe(false)
  })

  it('modal positioning and sizing', async () => {
    const positions = ['center', 'top', 'bottom'] as const
    const sizes = ['sm', 'md', 'lg', 'xl', 'full'] as const

    for (const position of positions) {
      for (const size of sizes) {
        const showModal = ref(false)

        const wrapper = mount({
          template: `
            <AppModal
              v-model="showModal"
              title="Test Modal"
              :position="position"
              :size="size"
            >
              <p>Modal content</p>
            </AppModal>
          `,
          components: { AppModal },
          setup() {
            return {
              showModal,
              position,
              size
            }
          }
        })

        // Open modal
        showModal.value = true
        await wrapper.vm.$nextTick()

        const modal = wrapper.findComponent(AppModal)
        expect(modal.props('position')).toBe(position)
        expect(modal.props('size')).toBe(size)

        showModal.value = false
      }
    }
  })

  it('modal with loading state', async () => {
    const showModal = ref(false)
    const loading = ref(false)

    const wrapper = mount({
      template: `
        <AppModal
          v-model="showModal"
          title="Save Data"
        >
          <p>Click save to simulate API call</p>
          <template #footer>
            <AppButton
              label="Save"
              :loading="loading"
              @click="onSave"
            />
          </template>
        </AppModal>
      `,
      components: { AppModal, AppButton },
      setup() {
        return {
          showModal,
          loading,
          onSave() {
            loading.value = true
            setTimeout(() => {
              loading.value = false
              showModal.value = false
            }, 100)
          }
        }
      }
    })

    // Open modal
    showModal.value = true
    await wrapper.vm.$nextTick()

    // Click save
    await wrapper.find('.btn--primary').trigger('click')

    // Loading state should be visible
    expect(loading.value).toBe(true)

    // Wait for save to complete
    await new Promise(resolve => setTimeout(resolve, 150))

    // Modal should be closed
    expect(showModal.value).toBe(false)
  })

  it('modal events emission', async () => {
    const showModal = ref(false)

    const wrapper = mount({
      template: `
        <AppModal
          v-model="showModal"
          title="Test Modal"
          @close="onClose"
          @esc="onEsc"
        >
          <p>Modal content</p>
        </AppModal>
      `,
      components: { AppModal },
      setup() {
        return {
          showModal,
          onClose: vi.fn(),
          onEsc: vi.fn()
        }
      }
    })

    // Open modal
    showModal.value = true
    await wrapper.vm.$nextTick()

    // Close via ESC
    const modal = wrapper.findComponent(AppModal)
    await modal.trigger('keydown', { key: 'Escape' })

    expect(wrapper.vm.onEsc).toHaveBeenCalled()

    // Open again and close via backdrop
    showModal.value = true
    await wrapper.vm.$nextTick()

    const backdrop = wrapper.find('.modal__backdrop')
    await backdrop.trigger('click')

    expect(wrapper.vm.onClose).toHaveBeenCalled()
  })

  it('modal in dark mode', async () => {
    document.documentElement.setAttribute('data-theme', 'dark')

    const showModal = ref(false)

    const wrapper = mount({
      template: `
        <AppModal
          v-model="showModal"
          title="Dark Mode Modal"
        >
          <p>This modal works in dark mode</p>
        </AppModal>
      `,
      components: { AppModal },
      setup() {
        return {
          showModal
        }
      }
    })

    // Open modal
    showModal.value = true
    await wrapper.vm.$nextTick()

    expect(wrapper.findComponent(AppModal).props('modelValue')).toBe(true)

    document.documentElement.removeAttribute('data-theme')
  })
})
