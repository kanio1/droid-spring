import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, RouterLinkStub } from '@vue/test-utils'
import AppDataTable from '~/components/ui/AppDataTable.vue'

// Mock scroll into view
Element.prototype.scrollIntoView = vi.fn()

describe('AppDataTable', () => {
  const columns = [
    { key: 'id', label: 'ID', sortable: true },
    { key: 'name', label: 'Name', sortable: true },
    { key: 'email', label: 'Email', sortable: false },
    { key: 'status', label: 'Status', sortable: true }
  ]

  const data = [
    { id: 1, name: 'John Doe', email: 'john@example.com', status: 'active' },
    { id: 2, name: 'Jane Smith', email: 'jane@example.com', status: 'inactive' },
    { id: 3, name: 'Bob Johnson', email: 'bob@example.com', status: 'active' }
  ]

  it('renders table with columns', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data }
    })

    const table = wrapper.find('.data-table')
    expect(table.exists()).toBe(true)

    const headers = wrapper.findAll('.data-table__header-cell')
    expect(headers.length).toBe(columns.length)
  })

  it('renders data rows', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data }
    })

    const rows = wrapper.findAll('.data-table__row')
    expect(rows.length).toBe(data.length)
  })

  it('displays cell values', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data }
    })

    const firstRow = wrapper.findAll('.data-table__row')[0]
    const cells = firstRow.findAll('.data-table__cell')
    expect(cells[0].text()).toBe('1')
    expect(cells[1].text()).toBe('John Doe')
    expect(cells[2].text()).toBe('john@example.com')
  })

  it('applies sort to column when header is clicked', async () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data }
    })

    const nameHeader = wrapper.findAll('.data-table__header-cell')[1]
    await nameHeader.trigger('click')

    expect(nameHeader.classes()).toContain('data-table__header-cell--sorted')
    expect(nameHeader.classes()).toContain('data-table__header-cell--asc')
  })

  it('toggles sort direction on second click', async () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data }
    })

    const nameHeader = wrapper.findAll('.data-table__header-cell')[1]
    await nameHeader.trigger('click')
    await nameHeader.trigger('click')

    expect(nameHeader.classes()).toContain('data-table__header-cell--sorted')
    expect(nameHeader.classes()).toContain('data-table__header-cell--desc')
  })

  it('removes sort from other columns', async () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data }
    })

    const nameHeader = wrapper.findAll('.data-table__header-cell')[1]
    await nameHeader.trigger('click')

    const idHeader = wrapper.findAll('.data-table__header-cell')[0]
    await idHeader.trigger('click')

    expect(nameHeader.classes()).not.toContain('data-table__header-cell--sorted')
    expect(idHeader.classes()).toContain('data-table__header-cell--sorted')
  })

  it('emits sort-change event when sort changes', async () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data }
    })

    const nameHeader = wrapper.findAll('.data-table__header-cell')[1]
    await nameHeader.trigger('click')

    expect(wrapper.emitted('sort-change')).toBeTruthy()
    expect(wrapper.emitted('sort-change')[0]).toEqual([{ key: 'name', direction: 'asc' }])
  })

  it('does not allow sorting on non-sortable columns', async () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data }
    })

    const emailHeader = wrapper.findAll('.data-table__header-cell')[2]
    await emailHeader.trigger('click')

    expect(emailHeader.classes()).not.toContain('data-table__header-cell--sorted')
    expect(wrapper.emitted('sort-change')).toBeUndefined()
  })

  it('applies custom sort comparator', async () => {
    const customSort = vi.fn((a, b) => a.id - b.id)
    const wrapper = mount(AppDataTable, {
      props: { columns, data, sortComparator: customSort }
    })

    const idHeader = wrapper.findAll('.data-table__header-cell')[0]
    await idHeader.trigger('click')

    expect(customSort).toHaveBeenCalled()
  })

  it('enables row selection', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, selectable: true }
    })

    const checkboxes = wrapper.findAll('.data-table__checkbox')
    expect(checkboxes.length).toBe(data.length + 1) // +1 for header checkbox
  })

  it('selects all rows when header checkbox is checked', async () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, selectable: true }
    })

    const headerCheckbox = wrapper.find('.data-table__checkbox--header')
    await headerCheckbox.trigger('change', { target: { checked: true } })

    expect(wrapper.emitted('select-all')).toBeTruthy()
    expect(wrapper.emitted('select-all')[0][0]).toEqual(data.map(item => item.id))
  })

  it('selects individual row', async () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, selectable: true }
    })

    const firstRowCheckbox = wrapper.findAll('.data-table__checkbox--row')[0]
    await firstRowCheckbox.trigger('change', { target: { checked: true } })

    expect(wrapper.emitted('select-row')).toBeTruthy()
    expect(wrapper.emitted('select-row')[0]).toEqual([data[0].id])
  })

  it('shows row actions', () => {
    const rowActions = [
      { label: 'Edit', action: vi.fn() },
      { label: 'Delete', action: vi.fn() }
    ]

    const wrapper = mount(AppDataTable, {
      props: { columns, data, rowActions }
    })

    const actionsColumn = wrapper.find('.data-table__actions')
    expect(actionsColumn.exists()).toBe(true)

    const actionButtons = wrapper.findAll('.data-table__action-button')
    expect(actionButtons.length).toBe(data.length * rowActions.length)
  })

  it('emits action event when row action is clicked', async () => {
    const editAction = vi.fn()
    const rowActions = [
      { label: 'Edit', action: editAction, icon: 'lucide:edit' }
    ]

    const wrapper = mount(AppDataTable, {
      props: { columns, data, rowActions }
    })

    const firstRowEditButton = wrapper.findAll('.data-table__action-button')[0]
    await firstRowEditButton.trigger('click')

    expect(editAction).toHaveBeenCalledWith(data[0])
  })

  it('applies fixed layout', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, fixedLayout: true }
    })

    const table = wrapper.find('.data-table')
    expect(table.classes()).toContain('data-table--fixed-layout')
  })

  it('applies sticky header', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, stickyHeader: true }
    })

    const header = wrapper.find('.data-table__header')
    expect(header.classes()).toContain('data-table__header--sticky')
  })

  it('applies compact variant', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, density: 'compact' }
    })

    const table = wrapper.find('.data-table')
    expect(table.classes()).toContain('data-table--compact')
  })

  it('applies cozy variant', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, density: 'cozy' }
    })

    const table = wrapper.find('.data-table')
    expect(table.classes()).toContain('data-table--cozy')
  })

  it('applies comfortable variant', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, density: 'comfortable' }
    })

    const table = wrapper.find('.data-table')
    expect(table.classes()).toContain('data-table--comfortable')
  })

  it('shows empty state when no data', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data: [] }
    })

    const emptyState = wrapper.find('.data-table__empty')
    expect(emptyState.exists()).toBe(true)
  })

  it('applies custom cell formatter', () => {
    const columnsWithFormatter = [
      {
        key: 'status',
        label: 'Status',
        formatter: (value: string) => value.toUpperCase()
      }
    ]

    const dataWithStatus = [
      { id: 1, name: 'John', status: 'active' }
    ]

    const wrapper = mount(AppDataTable, {
      props: { columns: columnsWithFormatter, data: dataWithStatus }
    })

    const statusCell = wrapper.find('.data-table__cell')
    expect(statusCell.text()).toBe('ACTIVE')
  })

  it('applies custom row class', () => {
    const wrapper = mount(AppDataTable, {
      props: {
        columns,
        data,
        getRowClass: (row: any) => (row.status === 'active' ? 'row-active' : '')
      }
    })

    const firstRow = wrapper.findAll('.data-table__row')[0]
    expect(firstRow.classes()).toContain('row-active')
  })

  it('applies custom cell class', () => {
    const columnsWithCellClass = [
      {
        key: 'status',
        label: 'Status',
        cellClass: 'status-cell'
      }
    ]

    const dataWithStatus = [
      { id: 1, name: 'John', status: 'active' }
    ]

    const wrapper = mount(AppDataTable, {
      props: { columns: columnsWithCellClass, data: dataWithStatus }
    })

    const statusCell = wrapper.find('.status-cell')
    expect(statusCell.exists()).toBe(true)
  })

  it('handles loading state', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, loading: true }
    })

    const loader = wrapper.find('.data-table__loader')
    expect(loader.exists()).toBe(true)
  })

  it('shows loading overlay', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, loading: true }
    })

    const overlay = wrapper.find('.data-table__loading-overlay')
    expect(overlay.exists()).toBe(true)
  })

  it('emits row-click event when row is clicked', async () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data }
    })

    const firstRow = wrapper.findAll('.data-table__row')[0]
    await firstRow.trigger('click')

    expect(wrapper.emitted('row-click')).toBeTruthy()
    expect(wrapper.emitted('row-click')[0]).toEqual([data[0]])
  })

  it('does not emit row-click when clicking on interactive elements', async () => {
    const rowActions = [
      { label: 'Edit', action: vi.fn() }
    ]

    const wrapper = mount(AppDataTable, {
      props: { columns, data, rowActions }
    })

    const actionButton = wrapper.find('.data-table__action-button')
    await actionButton.trigger('click')

    expect(wrapper.emitted('row-click')).toBeUndefined()
  })

  it('applies zebra striping', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, zebraStriping: true }
    })

    const table = wrapper.find('.data-table')
    expect(table.classes()).toContain('data-table--zebra')
  })

  it('applies hover state', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, hoverable: true }
    })

    const table = wrapper.find('.data-table')
    expect(table.classes()).toContain('data-table--hover')
  })

  it('renders with custom column width', () => {
    const columnsWithWidth = [
      { key: 'id', label: 'ID', width: '100px' },
      { key: 'name', label: 'Name', width: '200px' }
    ]

    const wrapper = mount(AppDataTable, {
      props: { columns: columnsWithWidth, data }
    })

    const firstHeader = wrapper.findAll('.data-table__header-cell')[0]
    expect(firstHeader.attributes('style')).toContain('width: 100px')
  })

  it('handles column hiding', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, hideColumns: ['email'] }
    })

    const emailHeader = wrapper.find('.data-table__header-cell--email')
    expect(emailHeader.exists()).toBe(false)
  })

  it('applies virtual scroll for large datasets', () => {
    const largeData = Array.from({ length: 1000 }, (_, i) => ({
      id: i,
      name: `User ${i}`,
      email: `user${i}@example.com`,
      status: i % 2 === 0 ? 'active' : 'inactive'
    }))

    const wrapper = mount(AppDataTable, {
      props: { columns, data: largeData, virtualScroll: true }
    })

    const table = wrapper.find('.data-table')
    expect(table.classes()).toContain('data-table--virtual')
  })

  it('exports data', async () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, exportable: true }
    })

    const exportButton = wrapper.find('.data-table__export-button')
    await exportButton.trigger('click')

    expect(wrapper.emitted('export')).toBeTruthy()
  })

  it('applies custom empty message', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data: [], emptyMessage: 'No items found' }
    })

    const emptyState = wrapper.find('.data-table__empty')
    expect(emptyState.text()).toBe('No items found')
  })

  it('applies custom class via class prop', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, class: 'custom-table' }
    })

    const table = wrapper.find('.data-table')
    expect(table.classes()).toContain('custom-table')
  })

  it('applies id when provided', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data, id: 'custom-table' }
    })

    const table = wrapper.find('.data-table')
    expect(table.attributes('id')).toBe('custom-table')
  })

  it('generates id when not provided', () => {
    const wrapper = mount(AppDataTable, {
      props: { columns, data }
    })

    const table = wrapper.find('.data-table')
    expect(table.attributes('id')).toBeDefined()
  })

  it('renders correctly in dark mode', () => {
    document.documentElement.setAttribute('data-theme', 'dark')

    const wrapper = mount(AppDataTable, {
      props: { columns, data }
    })

    const table = wrapper.find('.data-table')
    expect(table.exists()).toBe(true)

    document.documentElement.removeAttribute('data-theme')
  })

  it('snapshots in different states', () => {
    const states = [
      { columns, data },
      { columns, data, selectable: true },
      { columns, data, loading: true },
      { columns, data: [], emptyMessage: 'No data' },
      {
        columns,
        data,
        rowActions: [{ label: 'Edit', action: vi.fn() }]
      }
    ]

    states.forEach(state => {
      const wrapper = mount(AppDataTable, { props: state })
      expect(wrapper.element).toMatchSnapshot()
    })
  })
})
