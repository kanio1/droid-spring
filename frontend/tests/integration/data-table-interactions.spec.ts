import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'

// Import components
import AppDataTable from '~/components/ui/AppDataTable.vue'
import AppButton from '~/components/ui/AppButton.vue'
import AppBadge from '~/components/ui/AppBadge.vue'
import AppModal from '~/components/ui/AppModal.vue'
import AppSelect from '~/components/ui/AppSelect.vue'
import AppInput from '~/components/ui/AppInput.vue'

describe('Data Table Interactions Integration', () => {
  const columns = [
    { key: 'id', label: 'ID', sortable: true, width: '80px' },
    { key: 'name', label: 'Name', sortable: true },
    { key: 'email', label: 'Email', sortable: false },
    { key: 'status', label: 'Status', sortable: true },
    { key: 'role', label: 'Role', sortable: true }
  ]

  const data = [
    { id: 1, name: 'John Doe', email: 'john@example.com', status: 'active', role: 'admin' },
    { id: 2, name: 'Jane Smith', email: 'jane@example.com', status: 'inactive', role: 'user' },
    { id: 3, name: 'Bob Johnson', email: 'bob@example.com', status: 'active', role: 'user' },
    { id: 4, name: 'Alice Brown', email: 'alice@example.com', status: 'active', role: 'moderator' },
    { id: 5, name: 'Charlie Davis', email: 'charlie@example.com', status: 'inactive', role: 'user' }
  ]

  it('complete table with sorting and selection', async () => {
    const wrapper = mount({
      template: `
        <div>
          <AppDataTable
            :columns="columns"
            :data="data"
            :selectable="true"
            :row-actions="rowActions"
            @sort-change="onSortChange"
            @select-row="onSelectRow"
            @select-all="onSelectAll"
            @row-click="onRowClick"
            @action="onAction"
          />
        </div>
      `,
      components: { AppDataTable },
      data() {
        return {
          columns: ${JSON.stringify(columns)},
          data: ${JSON.stringify(data)},
          rowActions: [
            { label: 'Edit', action: vi.fn() },
            { label: 'Delete', action: vi.fn() }
          ]
        }
      },
      methods: {
        onSortChange: vi.fn(),
        onSelectRow: vi.fn(),
        onSelectAll: vi.fn(),
        onRowClick: vi.fn(),
        onAction: vi.fn()
      }
    })

    // Initial render
    expect(wrapper.find('.data-table').exists()).toBe(true)
    expect(wrapper.findAll('.data-table__row').length).toBe(data.length)

    // Sort by name
    const nameHeader = wrapper.findAll('.data-table__header-cell')[1]
    await nameHeader.trigger('click')

    expect(nameHeader.classes()).toContain('data-table__header-cell--sorted')
    expect(nameHeader.classes()).toContain('data-table__header-cell--asc')

    // Change sort direction
    await nameHeader.trigger('click')
    expect(nameHeader.classes()).toContain('data-table__header-cell--desc')

    // Select a row
    const firstRowCheckbox = wrapper.findAll('.data-table__checkbox--row')[0]
    await firstRowCheckbox.trigger('change', { target: { checked: true } })

    expect(wrapper.vm.onSelectRow).toHaveBeenCalledWith(data[0].id)

    // Select all
    const headerCheckbox = wrapper.find('.data-table__checkbox--header')
    await headerCheckbox.trigger('change', { target: { checked: true } })

    expect(wrapper.vm.onSelectAll).toHaveBeenCalledWith(data.map(item => item.id))
  })

  it('table with custom cell formatters', async () => {
    const columnsWithFormatters = [
      ...columns,
      {
        key: 'status',
        label: 'Status',
        formatter: (value: string) => {
          const variants = {
            active: 'success',
            inactive: 'danger'
          } as Record<string, string>
          return variants[value] || 'neutral'
        }
      }
    ]

    const wrapper = mount({
      template: `
        <AppDataTable
          :columns="columns"
          :data="data"
        />
      `,
      components: { AppDataTable },
      data() {
        return {
          columns: columnsWithFormatters,
          data
        }
      }
    })

    // Check that status is formatted with badges
    const statusCell = wrapper.findAll('.data-table__cell')[3]
    expect(statusCell.findComponent(AppBadge).exists()).toBe(true)
    expect(statusCell.findComponent(AppBadge).props('variant')).toBe('success')
  })

  it('table with action modal', async () => {
    const showEditModal = ref(false)
    const selectedCustomer = ref<any>(null)

    const wrapper = mount({
      template: `
        <div>
          <AppDataTable
            :columns="columns"
            :data="data"
            :row-actions="rowActions"
            @action="handleAction"
          />

          <AppModal
            v-model="showEditModal"
            title="Edit Customer"
          >
            <div v-if="selectedCustomer">
              <p>Editing: {{ selectedCustomer.name }}</p>
            </div>
            <template #footer>
              <AppButton
                label="Cancel"
                variant="outline"
                @click="showEditModal = false"
              />
              <AppButton
                label="Save"
                @click="saveChanges"
              />
            </template>
          </AppModal>
        </div>
      `,
      components: { AppDataTable, AppModal, AppButton },
      data() {
        return {
          columns,
          data,
          rowActions: [
            { label: 'Edit', action: 'edit' },
            { label: 'Delete', action: 'delete' }
          ]
        }
      },
      setup() {
        return {
          showEditModal,
          selectedCustomer,
          handleAction(action: string, row: any) {
            if (action === 'edit') {
              selectedCustomer.value = row
              showEditModal.value = true
            }
          },
          saveChanges() {
            showEditModal.value = false
          }
        }
      }
    })

    // Click edit action
    const editButton = wrapper.findAll('.data-table__action-button')[0]
    await editButton.trigger('click')

    // Modal should open
    expect(showEditModal.value).toBe(true)
    expect(selectedCustomer.value).toEqual(data[0])

    // Close modal
    await wrapper.findAll('button').at(-1).trigger('click')
    expect(showEditModal.value).toBe(false)
  })

  it('table with search and filter', async () => {
    const searchTerm = ref('')
    const statusFilter = ref('')

    const wrapper = mount({
      template: `
        <div>
          <div class="table-filters">
            <AppInput
              v-model="searchTerm"
              placeholder="Search customers..."
              icon="lucide:search"
            />
            <AppSelect
              v-model="statusFilter"
              :options="statusOptions"
              placeholder="Filter by status"
              clearable
            />
          </div>
          <AppDataTable
            :columns="columns"
            :data="filteredData"
          />
        </div>
      `,
      components: { AppInput, AppSelect, AppDataTable },
      data() {
        return {
          columns,
          data,
          statusOptions: [
            { label: 'Active', value: 'active' },
            { label: 'Inactive', value: 'inactive' }
          ]
        }
      },
      computed: {
        filteredData() {
          let result = [...this.data]

          if (this.searchTerm) {
            const term = this.searchTerm.toLowerCase()
            result = result.filter(item =>
              item.name.toLowerCase().includes(term) ||
              item.email.toLowerCase().includes(term)
            )
          }

          if (this.statusFilter) {
            result = result.filter(item => item.status === this.statusFilter)
          }

          return result
        }
      },
      setup() {
        return {
          searchTerm,
          statusFilter
        }
      }
    })

    // Initial count
    expect(wrapper.findAll('.data-table__row').length).toBe(5)

    // Search for "john"
    await wrapper.find('input').setValue('john')
    await wrapper.vm.$nextTick()

    expect(wrapper.findAll('.data-table__row').length).toBe(1)
    expect(wrapper.text()).toContain('John Doe')

    // Clear search
    await wrapper.find('input').setValue('')

    // Filter by status
    await wrapper.find('.select__trigger').trigger('click')
    await wrapper.findAll('.select__option')[0].trigger('click')

    await wrapper.vm.$nextTick()
    const activeRows = wrapper.findAll('.data-table__row')
    expect(activeRows.length).toBe(3) // 3 active customers
  })

  it('table with bulk actions', async () => {
    const selectedIds = ref<number[]>([])
    const showBulkModal = ref(false)

    const wrapper = mount({
      template: `
        <div>
          <div v-if="selectedIds.length" class="bulk-actions">
            <AppBadge :label="selectedIds.length + ' selected'" />
            <AppButton
              label="Bulk Delete"
              variant="danger"
              @click="showBulkModal = true"
            />
          </div>

          <AppDataTable
            :columns="columns"
            :data="data"
            :selectable="true"
            @select-row="onSelectRow"
          />

          <AppModal
            v-model="showBulkModal"
            title="Bulk Delete"
            variant="alert"
          >
            <p>Are you sure you want to delete {{ selectedIds.length }} customers?</p>
            <template #footer>
              <AppButton
                label="Cancel"
                variant="outline"
                @click="showBulkModal = false"
              />
              <AppButton
                label="Delete"
                variant="danger"
                @click="performBulkDelete"
              />
            </template>
          </AppModal>
        </div>
      `,
      components: { AppDataTable, AppButton, AppBadge, AppModal },
      data() {
        return {
          columns,
          data
        }
      },
      setup() {
        return {
          selectedIds,
          showBulkModal,
          onSelectRow(id: number) {
            const index = selectedIds.value.indexOf(id)
            if (index > -1) {
              selectedIds.value.splice(index, 1)
            } else {
              selectedIds.value.push(id)
            }
          },
          performBulkDelete() {
            // Simulate delete
            selectedIds.value = []
            showBulkModal.value = false
          }
        }
      }
    })

    // Select first two rows
    const firstRowCheckbox = wrapper.findAll('.data-table__checkbox--row')[0]
    await firstRowCheckbox.trigger('change', { target: { checked: true } })

    const secondRowCheckbox = wrapper.findAll('.data-table__checkbox--row')[1]
    await secondRowCheckbox.trigger('change', { target: { checked: true } })

    // Bulk actions should be visible
    expect(wrapper.find('.bulk-actions').exists()).toBe(true)
    expect(wrapper.findComponent(AppBadge).props('label')).toBe('2 selected')

    // Click bulk delete
    await wrapper.find('.btn--danger').trigger('click')

    // Confirmation modal
    expect(wrapper.findComponent(AppModal).props('modelValue')).toBe(true)

    // Confirm delete
    await wrapper.findAll('button').at(-1).trigger('click')
    expect(showBulkModal.value).toBe(false)
    expect(selectedIds.value.length).toBe(0)
  })

  it('table with row click handling', async () => {
    const selectedCustomer = ref<any>(null)
    const showDetailsModal = ref(false)

    const wrapper = mount({
      template: `
        <AppDataTable
          :columns="columns"
          :data="data"
          :hoverable="true"
          @row-click="handleRowClick"
        />

        <AppModal
          v-model="showDetailsModal"
          title="Customer Details"
        >
          <div v-if="selectedCustomer">
            <p><strong>ID:</strong> {{ selectedCustomer.id }}</p>
            <p><strong>Name:</strong> {{ selectedCustomer.name }}</p>
            <p><strong>Email:</strong> {{ selectedCustomer.email }}</p>
            <p><strong>Status:</strong> {{ selectedCustomer.status }}</p>
          </div>
        </AppModal>
      `,
      components: { AppDataTable, AppModal },
      data() {
        return {
          columns,
          data
        }
      },
      setup() {
        return {
          selectedCustomer,
          showDetailsModal,
          handleRowClick(row: any) {
            selectedCustomer.value = row
            showDetailsModal.value = true
          }
        }
      }
    })

    // Click on a row
    const firstRow = wrapper.findAll('.data-table__row')[0]
    await firstRow.trigger('click')

    // Modal should open with details
    expect(showDetailsModal.value).toBe(true)
    expect(selectedCustomer.value).toEqual(data[0])

    // Close modal
    showDetailsModal.value = false
    await wrapper.vm.$nextTick()
    expect(wrapper.findComponent(AppModal).props('modelValue')).toBe(false)
  })

  it('table with custom row class', async () => {
    const wrapper = mount({
      template: `
        <AppDataTable
          :columns="columns"
          :data="data"
          :get-row-class="getRowClass"
        />
      `,
      components: { AppDataTable },
      data() {
        return {
          columns,
          data
        }
      },
      methods: {
        getRowClass(row: any) {
          return row.status === 'active' ? 'row-active' : 'row-inactive'
        }
      }
    })

    // Check row styling
    const rows = wrapper.findAll('.data-table__row')
    expect(rows[0].classes()).toContain('row-active') // John is active
    expect(rows[1].classes()).toContain('row-inactive') // Jane is inactive
  })

  it('table with empty state and actions', async () => {
    const showCreateModal = ref(false)

    const wrapper = mount({
      template: `
        <div>
          <div class="table-header">
            <h2>Customers</h2>
            <AppButton
              label="Add Customer"
              @click="showCreateModal = true"
            />
          </div>

          <AppDataTable
            :columns="columns"
            :data="data"
            empty-message="No customers found. Create your first customer!"
          />

          <AppModal
            v-model="showCreateModal"
            title="Add Customer"
          >
            <p>Customer creation form would go here</p>
          </AppModal>
        </div>
      `,
      components: { AppDataTable, AppButton, AppModal },
      data() {
        return {
          columns,
          data: []
        }
      },
      setup() {
        return {
          showCreateModal
        }
      }
    })

    // Empty state
    expect(wrapper.find('.data-table__empty').exists()).toBe(true)
    expect(wrapper.text()).toContain('No customers found')

    // Add button
    const addButton = wrapper.find('.btn--primary')
    await addButton.trigger('click')

    // Create modal
    expect(showCreateModal.value).toBe(true)
  })

  it('table in dark mode', async () => {
    document.documentElement.setAttribute('data-theme', 'dark')

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

    // Table should render in dark mode
    expect(wrapper.find('.data-table').exists()).toBe(true)
    expect(wrapper.find('.data-table__header').exists()).toBe(true)
    expect(wrapper.findAll('.data-table__row').length).toBe(5)

    document.documentElement.removeAttribute('data-theme')
  })
})
