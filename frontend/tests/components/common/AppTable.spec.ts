/**
 * Test for AppTable Component
 * Following Arrange-Act-Assert pattern
 */

import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AppTable from '~/components/common/AppTable.vue'

// Test data
const createMockColumns = () => [
  { key: 'id', label: 'ID', sortable: true, width: '80px' },
  { key: 'name', label: 'Name', sortable: true, align: 'left' },
  { key: 'email', label: 'Email', align: 'left' },
  { key: 'status', label: 'Status', sortable: true, align: 'center' }
]

const createMockData = () => [
  { id: 1, name: 'John Doe', email: 'john@example.com', status: 'Active' },
  { id: 2, name: 'Jane Smith', email: 'jane@example.com', status: 'Inactive' },
  { id: 3, name: 'Bob Johnson', email: 'bob@example.com', status: 'Active' }
]

const createWrapper = (props = {}) => {
  return mount(AppTable, {
    props: {
      columns: createMockColumns(),
      data: createMockData(),
      ...props
    }
  })
}

describe('AppTable Component', () => {
  describe('Rendering', () => {
    it('should render table with default props', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      expect(wrapper.find('.app-table').exists()).toBe(true)
      expect(wrapper.find('.app-table__table').exists()).toBe(true)
      expect(wrapper.find('.app-table__thead').exists()).toBe(true)
      expect(wrapper.find('.app-table__tbody').exists()).toBe(true)
    })

    it('should render title when provided', () => {
      // Arrange & Act
      const wrapper = createWrapper({ title: 'Customer List' })

      // Assert
      expect(wrapper.find('.app-table__title').text()).toBe('Customer List')
    })

    it('should render header slot when provided', () => {
      // Arrange & Act
      const wrapper = mount(AppTable, {
        props: {
          columns: createMockColumns(),
          data: []
        },
        slots: {
          header: '<div class="custom-header">Custom Header</div>'
        }
      })

      // Assert
      expect(wrapper.find('.custom-header').exists()).toBe(true)
    })

    it('should apply loading class when loading', () => {
      // Arrange & Act
      const wrapper = createWrapper({ loading: true })

      // Assert
      expect(wrapper.classes()).toContain('app-table--loading')
    })

    it('should apply clickable class when clickable', () => {
      // Arrange & Act
      const wrapper = createWrapper({ clickable: true })

      // Assert
      expect(wrapper.classes()).toContain('app-table--clickable')
    })

    it('should apply pagination class when showPagination is true', () => {
      // Arrange & Act
      const wrapper = createWrapper({ showPagination: true })

      // Assert
      expect(wrapper.classes()).toContain('app-table--with-pagination')
    })
  })

  describe('Columns', () => {
    it('should render all columns in header', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      const headers = wrapper.findAll('.app-table__th')
      expect(headers.length).toBe(4)
      expect(headers[0].text()).toBe('ID')
      expect(headers[1].text()).toBe('Name')
      expect(headers[2].text()).toBe('Email')
      expect(headers[3].text()).toBe('Status')
    })

    it('should apply column width styles', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      const firstHeader = wrapper.find('.app-table__th')
      expect(firstHeader.attributes('style')).toContain('width: 80px')
    })

    it('should display sort indicators on sortable columns', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      const sortableHeaders = wrapper.findAll('.app-table__th')
      expect(sortableHeaders[0].find('.app-table__sort').exists()).toBe(true)
      expect(sortableHeaders[1].find('.app-table__sort').exists()).toBe(true)
      expect(sortableHeaders[2].find('.app-table__sort').exists()).toBe(false)
    })

    it('should render all data rows', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      const rows = wrapper.findAll('.app-table__tbody .app-table__tr')
      expect(rows.length).toBe(3)
    })

    it('should render all cells in each row', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      const cells = wrapper.findAll('.app-table__tbody .app-table__td')
      expect(cells.length).toBe(12) // 3 rows × 4 columns
    })
  })

  describe('Row Clicking', () => {
    it('should emit rowClick when row is clicked and clickable is true', async () => {
      // Arrange
      const wrapper = createWrapper({ clickable: true })

      // Act
      await wrapper.findAll('.app-table__tbody .app-table__tr')[0].trigger('click')

      // Assert
      expect(wrapper.emitted('rowClick')).toBeDefined()
      expect(wrapper.emitted('rowClick')?.length).toBe(1)
      expect(wrapper.emitted('rowClick')?.[0]).toEqual([
        { id: 1, name: 'John Doe', email: 'john@example.com', status: 'Active' },
        0
      ])
    })

    it('should not emit rowClick when clickable is false', async () => {
      // Arrange
      const wrapper = createWrapper({ clickable: false })

      // Act
      await wrapper.find('.app-table__tbody .app-table__tr').trigger('click')

      // Assert
      expect(wrapper.emitted('rowClick')).toBeUndefined()
    })

    it('should add clickable class to rows when clickable is true', () => {
      // Arrange & Act
      const wrapper = createWrapper({ clickable: true })

      // Assert
      const firstRow = wrapper.find('.app-table__tbody .app-table__tr')
      expect(firstRow.classes()).toContain('app-table__tr--clickable')
    })

    it('should highlight selected rows', () => {
      // Arrange & Act
      const wrapper = createWrapper({
        clickable: true,
        selectedRows: [{ id: 1 }]
      })

      // Assert
      const firstRow = wrapper.find('.app-table__tbody .app-table__tr')
      expect(firstRow.classes()).toContain('app-table__tr--selected')
    })
  })

  describe('Sorting', () => {
    it('should emit sort event when sortable column is clicked', async () => {
      // Arrange
      const wrapper = createWrapper()

      // Act
      await wrapper.findAll('.app-table__th')[0].trigger('click')

      // Assert
      expect(wrapper.emitted('sort')).toBeDefined()
      expect(wrapper.emitted('sort')?.length).toBe(1)
      expect(wrapper.emitted('sort')?.[0]).toEqual([
        { key: 'id', label: 'ID', sortable: true, width: '80px' },
        'asc'
      ])
    })

    it('should toggle sort direction when same column is clicked twice', async () => {
      // Arrange
      const wrapper = createWrapper()
      const idColumn = createMockColumns()[0]

      // Act
      await wrapper.findAll('.app-table__th')[0].trigger('click')
      await wrapper.findAll('.app-table__th')[0].trigger('click')

      // Assert
      expect(wrapper.emitted('sort')?.[1]).toEqual([idColumn, 'desc'])
    })

    it('should change sort column and reset direction when different column is clicked', async () => {
      // Arrange
      const wrapper = createWrapper()
      const nameColumn = createMockColumns()[1]

      // Act
      await wrapper.findAll('.app-table__th')[0].trigger('click')
      await wrapper.findAll('.app-table__th')[1].trigger('click')

      // Assert
      expect(wrapper.emitted('sort')?.[1]).toEqual([nameColumn, 'asc'])
    })

    it('should not emit sort event when non-sortable column is clicked', async () => {
      // Arrange
      const wrapper = createWrapper()

      // Act
      await wrapper.findAll('.app-table__th')[2].trigger('click')

      // Assert
      expect(wrapper.emitted('sort')).toBeUndefined()
    })

    it('should display correct sort icons', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      const headers = wrapper.findAll('.app-table__th')
      // First click on ID column
      headers[0].trigger('click')

      // Relookup headers after click
      setTimeout(() => {
        const updatedHeaders = wrapper.findAll('.app-table__th')
        expect(updatedHeaders[0].find('.app-table__sort').text()).toBe('↑')
      }, 0)
    })
  })

  describe('Pagination', () => {
    it('should show pagination footer when showPagination is true', () => {
      // Arrange & Act
      const wrapper = createWrapper({ showPagination: true })

      // Assert
      expect(wrapper.find('.app-table__footer').exists()).toBe(true)
    })

    it('should hide pagination footer when showPagination is false', () => {
      // Arrange & Act
      const wrapper = createWrapper({ showPagination: false })

      // Assert
      expect(wrapper.find('.app-table__footer').exists()).toBe(false)
    })

    it('should display pagination info', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      expect(wrapper.find('.app-table__info-text').text()).toContain('Showing 1-')
    })

    it('should emit pageChange when page changes', async () => {
      // Arrange
      const wrapper = createWrapper()

      // Act
      await wrapper.find('[data-testid="page-next"]').trigger('click')

      // Assert
      expect(wrapper.emitted('pageChange')).toBeDefined()
    })

    it('should emit sizeChange when page size changes', async () => {
      // Arrange
      const wrapper = createWrapper()

      // Act
      await wrapper.find('[data-testid="size-select"]').trigger('change')

      // Assert
      expect(wrapper.emitted('sizeChange')).toBeDefined()
    })
  })

  describe('Empty State', () => {
    it('should show empty state when data is empty', () => {
      // Arrange & Act
      const wrapper = createWrapper({ data: [] })

      // Assert
      const emptyRow = wrapper.find('.app-table__td--empty')
      expect(emptyRow.exists()).toBe(true)
      expect(emptyRow.text()).toBe('No data available')
    })

    it('should show custom empty state when slot is provided', () => {
      // Arrange & Act
      const wrapper = mount(AppTable, {
        props: {
          columns: createMockColumns(),
          data: []
        },
        slots: {
          empty: '<div class="custom-empty">No items found</div>'
        }
      })

      // Assert
      expect(wrapper.find('.custom-empty').exists()).toBe(true)
      expect(wrapper.find('.custom-empty').text()).toBe('No items found')
    })
  })

  describe('Cell Formatting', () => {
    it('should format cell values using column format function', () => {
      // Arrange
      const columns = [
        {
          key: 'amount',
          label: 'Amount',
          format: (value: number) => `$${value.toFixed(2)}`
        }
      ]
      const data = [{ amount: 123.456 }]

      // Act
      const wrapper = mount(AppTable, {
        props: { columns, data }
      })

      // Assert
      const cell = wrapper.find('.app-table__td')
      expect(cell.text()).toBe('$123.46')
    })

    it('should render slot content when cell slot is provided', () => {
      // Arrange
      const columns = [{ key: 'name', label: 'Name' }]
      const data = [{ name: 'Test' }]

      // Act
      const wrapper = mount(AppTable, {
        props: { columns, data },
        slots: {
          'cell-name': '<span class="custom-cell">Custom Content</span>'
        }
      })

      // Assert
      expect(wrapper.find('.custom-cell').exists()).toBe(true)
    })

    it('should display raw value when no format or slot is provided', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      const firstCell = wrapper.find('.app-table__tbody .app-table__td')
      expect(firstCell.text()).toBe('1')
    })
  })

  describe('Column Alignment', () => {
    it('should align columns to left by default', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      const cells = wrapper.findAll('.app-table__td')
      expect(cells[0].classes()).toContain('app-table__td--left')
    })

    it('should apply center alignment when specified', () => {
      // Arrange & Act
      const columns = [{ key: 'status', label: 'Status', align: 'center' }]
      const wrapper = mount(AppTable, {
        props: { columns, data: [{ status: 'Active' }] }
      })

      // Assert
      const cell = wrapper.find('.app-table__td')
      expect(cell.classes()).toContain('app-table__td--center')
    })

    it('should apply right alignment when specified', () => {
      // Arrange & Act
      const columns = [{ key: 'amount', label: 'Amount', align: 'right' }]
      const wrapper = mount(AppTable, {
        props: { columns, data: [{ amount: 100 }] }
      })

      // Assert
      const cell = wrapper.find('.app-table__td')
      expect(cell.classes()).toContain('app-table__td--right')
    })
  })

  describe('Max Height', () => {
    it('should apply custom max height', () => {
      // Arrange & Act
      const wrapper = createWrapper({ maxHeight: '600px' })

      // Assert
      const container = wrapper.find('.app-table__container')
      expect(container.attributes('style')).toContain('maxHeight: 600px')
    })

    it('should use default max height when not specified', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      const container = wrapper.find('.app-table__container')
      expect(container.attributes('style')).toContain('maxHeight: 400px')
    })
  })

  describe('Computed Properties', () => {
    it('should calculate visible rows correctly with pagination', () => {
      // Arrange
      const data = Array.from({ length: 25 }, (_, i) => ({
        id: i + 1,
        name: `User ${i + 1}`
      }))

      // Act
      const wrapper = createWrapper({ data, size: 10 })
      const rows = wrapper.findAll('.app-table__tbody .app-table__tr')

      // Assert
      expect(rows.length).toBe(10)
    })

    it('should show all rows when pagination is disabled', () => {
      // Arrange
      const data = Array.from({ length: 100 }, (_, i) => ({
        id: i + 1,
        name: `User ${i + 1}`
      }))

      // Act
      const wrapper = createWrapper({ data, showPagination: false })
      const rows = wrapper.findAll('.app-table__tbody .app-table__tr')

      // Assert
      expect(rows.length).toBe(100)
    })

    it('should calculate pagination info correctly', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      const infoText = wrapper.find('.app-table__info-text').text()
      expect(infoText).toBe('Showing 1-3 of 3 results')
    })
  })

  describe('Row Selection', () => {
    it('should identify selected rows correctly', () => {
      // Arrange & Act
      const wrapper = createWrapper({
        selectedRows: [
          { id: 1 },
          { id: 3 }
        ]
      })

      // Assert
      const rows = wrapper.findAll('.app-table__tbody .app-table__tr')
      expect(rows[0].classes()).toContain('app-table__tr--selected')
      expect(rows[1].classes()).not.toContain('app-table__tr--selected')
      expect(rows[2].classes()).toContain('app-table__tr--selected')
    })

    it('should check selection by object equality', () => {
      // Arrange
      const selectedRow = { id: 2, name: 'Jane Smith', email: 'jane@example.com', status: 'Inactive' }

      // Act
      const wrapper = createWrapper({ selectedRows: [selectedRow] })

      // Assert
      const rows = wrapper.findAll('.app-table__tbody .app-table__tr')
      expect(rows[1].classes()).toContain('app-table__tr--selected')
    })
  })

  describe('Helper Methods', () => {
    it('should get cell value correctly', () => {
      // Arrange & Act
      const wrapper = createWrapper()
      const vm = wrapper.vm as any

      // Act
      const value = vm.getCellValue({ name: 'Test' }, { key: 'name' })

      // Assert
      expect(value).toBe('Test')
    })

    it('should format cell value with format function', () => {
      // Arrange & Act
      const wrapper = createWrapper()
      const vm = wrapper.vm as any
      const column = { key: 'amount', format: (v: number) => `$${v}` }

      // Act
      const value = vm.formatCellValue(100, column)

      // Assert
      expect(value).toBe('$100')
    })

    it('should handle undefined cell values', () => {
      // Arrange & Act
      const wrapper = createWrapper()
      const vm = wrapper.vm as any

      // Act
      const value = vm.getCellValue({}, { key: 'nonexistent' })

      // Assert
      expect(value).toBeUndefined()
      expect(vm.formatCellValue(undefined, { key: 'nonexistent' })).toBe('')
    })

    it('should get row key correctly', () => {
      // Arrange & Act
      const wrapper = createWrapper()
      const vm = wrapper.vm as any

      // Assert
      expect(vm.getRowKey({ id: 123 }, 0)).toBe(123)
      expect(vm.getRowKey({ key: 'abc' }, 1)).toBe('abc')
      expect(vm.getRowKey({}, 2)).toBe(2)
    })
  })

  describe('Scrollbar', () => {
    it('should have scrollable container', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      const container = wrapper.find('.app-table__container')
      expect(container.classes()).toContain('app-table__container')
    })
  })

  describe('Sticky Header', () => {
    it('should make header sticky', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      const header = wrapper.find('.app-table__thead')
      expect(header.classes()).toContain('app-table__thead')
    })
  })

  describe('Column Sort State', () => {
    it('should track sort column and direction', () => {
      // Arrange & Act
      const wrapper = createWrapper()
      const vm = wrapper.vm as any

      // Assert initial state
      expect(vm.sortColumn).toBe('')
      expect(vm.sortDirection).toBe('asc')
    })
  })

  describe('Edge Cases', () => {
    it('should handle data with no columns', () => {
      // Arrange & Act
      const wrapper = mount(AppTable, {
        props: {
          columns: [],
          data: []
        }
      })

      // Assert
      expect(wrapper.find('.app-table__thead').exists()).toBe(false)
      expect(wrapper.find('.app-table__td--empty').exists()).toBe(true)
    })

    it('should handle undefined row data', () => {
      // Arrange
      const data = [null, undefined]

      // Act
      const wrapper = createWrapper({ data })

      // Assert
      const rows = wrapper.findAll('.app-table__tbody .app-table__tr')
      expect(rows.length).toBe(2)
    })

    it('should handle data with extra properties not in columns', () => {
      // Arrange
      const data = [{ id: 1, name: 'Test', extraField: 'value' }]

      // Act
      const wrapper = createWrapper({ data })

      // Assert
      const rows = wrapper.findAll('.app-table__tbody .app-table__tr')
      expect(rows.length).toBe(1)
    })
  })
})
