import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useProductStore } from '~/stores/product'
import type { Product, ProductStatus, ProductType, ProductCategory } from '~/schemas/product'

// Mock useApi composable
const mockUseApi = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: mockUseApi
}))

describe('Product Store', () => {
  beforeEach(() => {
    mockUseApi.mockReturnValue({
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      del: vi.fn(),
      patch: vi.fn(),
      create: vi.fn(),
      read: vi.fn(),
      update: vi.fn(),
      remove: vi.fn(),
      paginatedGet: vi.fn(),
      request: vi.fn(),
      loading: vi.fn(() => false),
      handleSuccess: vi.fn(),
      buildUrl: vi.fn(),
      baseURL: 'http://localhost:8080/api'
    })
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should initialize with default state', () => {
    const store = useProductStore()

    expect(store.products).toEqual([])
    expect(store.currentProduct).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
    expect(store.pagination.page).toBe(0)
    expect(store.pagination.size).toBe(20)
    expect(store.pagination.totalElements).toBe(0)
  })

  it('should fetch products successfully', async () => {
    const store = useProductStore()
    const mockProducts: Product[] = [
      {
        id: '1',
        productCode: 'PROD001',
        name: 'Mobile Service Basic',
        description: 'Basic mobile service',
        productType: 'SERVICE' as ProductType,
        category: 'MOBILE' as ProductCategory,
        price: 29.99,
        currency: 'PLN',
        billingPeriod: 'monthly',
        status: 'ACTIVE' as ProductStatus,
        validityStart: null,
        validityEnd: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      },
      {
        id: '2',
        productCode: 'PROD002',
        name: 'Broadband Premium',
        description: 'Premium broadband package',
        productType: 'TARIFF' as ProductType,
        category: 'BROADBAND' as ProductCategory,
        price: 79.99,
        currency: 'PLN',
        billingPeriod: 'monthly',
        status: 'ACTIVE' as ProductStatus,
        validityStart: null,
        validityEnd: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockProducts,
        page: 0,
        size: 20,
        totalElements: 2,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 2,
        empty: false
      }
    } as any)

    await store.fetchProducts()

    expect(store.products).toEqual(mockProducts)
    expect(store.pagination.totalElements).toBe(2)
    expect(store.pagination.totalPages).toBe(1)
  })

  it('should filter products by status', async () => {
    const store = useProductStore()
    const mockProducts: Product[] = [
      {
        id: '1',
        productCode: 'PROD001',
        name: 'Mobile Service Basic',
        productType: 'SERVICE' as ProductType,
        category: 'MOBILE' as ProductCategory,
        price: 29.99,
        currency: 'PLN',
        billingPeriod: 'monthly',
        status: 'ACTIVE' as ProductStatus,
        validityStart: null,
        validityEnd: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockProducts,
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 1,
        empty: false
      }
    } as any)

    await store.fetchProducts({ status: 'ACTIVE' })

    expect(get).toHaveBeenCalledWith('/products', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        status: 'ACTIVE'
      }
    })
  })

  it('should filter products by type', async () => {
    const store = useProductStore()
    const mockProducts: Product[] = [
      {
        id: '1',
        productCode: 'PROD001',
        name: 'Mobile Service Basic',
        productType: 'SERVICE' as ProductType,
        category: 'MOBILE' as ProductCategory,
        price: 29.99,
        currency: 'PLN',
        billingPeriod: 'monthly',
        status: 'ACTIVE' as ProductStatus,
        validityStart: null,
        validityEnd: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockProducts,
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 1,
        empty: false
      }
    } as any)

    await store.fetchProducts({ type: 'SERVICE' })

    expect(get).toHaveBeenCalledWith('/products', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        type: 'SERVICE'
      }
    })
  })

  it('should create a product', async () => {
    const store = useProductStore()
    const newProduct = {
      productCode: 'PROD003',
      name: 'TV Package Premium',
      description: 'Premium TV package',
      productType: 'BUNDLE' as ProductType,
      category: 'TV' as ProductCategory,
      price: 99.99,
      currency: 'PLN',
      billingPeriod: 'monthly',
      status: 'ACTIVE' as ProductStatus
    }

    const mockProduct: Product = {
      id: '3',
      ...newProduct,
      validityStart: null,
      validityEnd: null,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    const { post } = mockUseApi()
    vi.mocked(post).mockResolvedValueOnce({
      data: mockProduct
    } as any)

    await store.createProduct(newProduct)

    expect(store.products).toHaveLength(1)
    expect(store.products[0]).toEqual(mockProduct)
    expect(post).toHaveBeenCalledWith('/products', newProduct)
  })

  it('should update a product', async () => {
    const store = useProductStore()

    const existingProduct: Product = {
      id: '1',
      productCode: 'PROD001',
      name: 'Mobile Service Basic',
      productType: 'SERVICE' as ProductType,
      category: 'MOBILE' as ProductCategory,
      price: 29.99,
      currency: 'PLN',
      billingPeriod: 'monthly',
      status: 'ACTIVE' as ProductStatus,
      validityStart: null,
      validityEnd: null,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.products = [existingProduct]

    const updateData = {
      id: '1',
      productCode: 'PROD001',
      name: 'Mobile Service Premium',
      description: 'Premium mobile service',
      productType: 'SERVICE' as ProductType,
      category: 'MOBILE' as ProductCategory,
      price: 49.99,
      currency: 'PLN',
      billingPeriod: 'monthly',
      status: 'ACTIVE' as ProductStatus,
      version: 1
    }

    const mockUpdatedProduct: Product = {
      ...existingProduct,
      ...updateData,
      updatedAt: '2024-01-02T00:00:00Z',
      version: 2
    }

    const { put } = mockUseApi()
    vi.mocked(put).mockResolvedValueOnce({
      data: mockUpdatedProduct
    } as any)

    await store.updateProduct(updateData)

    expect(store.products[0].name).toBe('Mobile Service Premium')
    expect(store.products[0].price).toBe(49.99)
    expect(store.products[0].version).toBe(2)
  })

  it('should delete a product', async () => {
    const store = useProductStore()

    const existingProduct: Product = {
      id: '1',
      productCode: 'PROD001',
      name: 'Mobile Service Basic',
      productType: 'SERVICE' as ProductType,
      category: 'MOBILE' as ProductCategory,
      price: 29.99,
      currency: 'PLN',
      billingPeriod: 'monthly',
      status: 'ACTIVE' as ProductStatus,
      validityStart: null,
      validityEnd: null,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.products = [existingProduct]
    store.pagination.totalElements = 1

    const { del } = mockUseApi()
    vi.mocked(del).mockResolvedValueOnce({} as any)

    await store.deleteProduct('1', 1)

    expect(store.products).toHaveLength(0)
    expect(store.pagination.totalElements).toBe(0)
    expect(del).toHaveBeenCalledWith('/products/1?version=1')
  })

  it('should change product status', async () => {
    const store = useProductStore()

    const existingProduct: Product = {
      id: '1',
      productCode: 'PROD001',
      name: 'Mobile Service Basic',
      productType: 'SERVICE' as ProductType,
      category: 'MOBILE' as ProductCategory,
      price: 29.99,
      currency: 'PLN',
      billingPeriod: 'monthly',
      status: 'ACTIVE' as ProductStatus,
      validityStart: null,
      validityEnd: null,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.products = [existingProduct]

    const statusData = {
      id: '1',
      status: 'INACTIVE' as ProductStatus
    }

    const mockUpdatedProduct: Product = {
      ...existingProduct,
      status: 'INACTIVE' as ProductStatus,
      updatedAt: '2024-01-02T00:00:00Z',
      version: 2
    }

    const { put } = mockUseApi()
    vi.mocked(put).mockResolvedValueOnce({
      data: mockUpdatedProduct
    } as any)

    await store.changeProductStatus(statusData)

    expect(store.products[0].status).toBe('INACTIVE')
    expect(store.products[0].version).toBe(2)
  })

  it('should get active products', async () => {
    const store = useProductStore()
    const mockProducts: Product[] = [
      {
        id: '1',
        productCode: 'PROD001',
        name: 'Mobile Service Basic',
        productType: 'SERVICE' as ProductType,
        category: 'MOBILE' as ProductCategory,
        price: 29.99,
        currency: 'PLN',
        billingPeriod: 'monthly',
        status: 'ACTIVE' as ProductStatus,
        validityStart: null,
        validityEnd: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockProducts,
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 1,
        empty: false
      }
    } as any)

    await store.getActiveProducts()

    expect(get).toHaveBeenCalledWith('/products', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        status: 'ACTIVE'
      }
    })
  })

  it('should compute filtered products correctly', () => {
    const store = useProductStore()

    const mockProducts: Product[] = [
      {
        id: '1',
        productCode: 'PROD001',
        name: 'Mobile Service',
        productType: 'SERVICE' as ProductType,
        category: 'MOBILE' as ProductCategory,
        price: 29.99,
        currency: 'PLN',
        billingPeriod: 'monthly',
        status: 'ACTIVE' as ProductStatus,
        validityStart: null,
        validityEnd: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      },
      {
        id: '2',
        productCode: 'PROD002',
        name: 'Broadband',
        productType: 'TARIFF' as ProductType,
        category: 'BROADBAND' as ProductCategory,
        price: 79.99,
        currency: 'PLN',
        billingPeriod: 'monthly',
        status: 'INACTIVE' as ProductStatus,
        validityStart: null,
        validityEnd: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      },
      {
        id: '3',
        productCode: 'PROD003',
        name: 'TV Package',
        productType: 'BUNDLE' as ProductType,
        category: 'TV' as ProductCategory,
        price: 99.99,
        currency: 'PLN',
        billingPeriod: 'monthly',
        status: 'DEPRECATED' as ProductStatus,
        validityStart: null,
        validityEnd: null,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    store.products = mockProducts

    expect(store.activeProducts).toHaveLength(1)
    expect(store.inactiveProducts).toHaveLength(1)
    expect(store.deprecatedProducts).toHaveLength(1)
    expect(store.serviceProducts).toHaveLength(1)
    expect(store.tariffProducts).toHaveLength(1)
    expect(store.bundleProducts).toHaveLength(1)
    expect(store.mobileProducts).toHaveLength(1)
    expect(store.broadbandProducts).toHaveLength(1)
    expect(store.tvProducts).toHaveLength(1)
  })
})
