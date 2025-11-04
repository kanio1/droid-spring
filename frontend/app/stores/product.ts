import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  Product,
  CreateProductCommand,
  UpdateProductCommand,
  ChangeProductStatusCommand,
  ProductSearchParams,
  ProductListResponse,
  ProductStatus,
  ProductType,
  ProductCategory
} from '~/schemas/product'

export const useProductStore = defineStore('product', () => {
  // State
  const products = ref<Product[]>([])
  const currentProduct = ref<Product | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pagination = reactive({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: false,
    numberOfElements: 0,
    empty: true
  })

  // Getters
  const productCount = computed(() => products.value.length)
  const activeProducts = computed(() => products.value.filter(p => p.status === 'ACTIVE'))
  const inactiveProducts = computed(() => products.value.filter(p => p.status === 'INACTIVE'))
  const deprecatedProducts = computed(() => products.value.filter(p => p.status === 'DEPRECATED'))

  const serviceProducts = computed(() => products.value.filter(p => p.productType === 'SERVICE'))
  const tariffProducts = computed(() => products.value.filter(p => p.productType === 'TARIFF'))
  const bundleProducts = computed(() => products.value.filter(p => p.productType === 'BUNDLE'))
  const addonProducts = computed(() => products.value.filter(p => p.productType === 'ADDON'))

  const mobileProducts = computed(() => products.value.filter(p => p.category === 'MOBILE'))
  const broadbandProducts = computed(() => products.value.filter(p => p.category === 'BROADBAND'))
  const tvProducts = computed(() => products.value.filter(p => p.category === 'TV'))
  const cloudProducts = computed(() => products.value.filter(p => p.category === 'CLOUD'))

  const getProductById = (id: string) => computed(() =>
    products.value.find(p => p.id === id)
  )

  const getProductsByType = (type: ProductType) => computed(() =>
    products.value.filter(p => p.productType === type)
  )

  const getProductsByCategory = (category: ProductCategory) => computed(() =>
    products.value.filter(p => p.category === category)
  )

  const getProductsByStatus = (status: ProductStatus) => computed(() =>
    products.value.filter(p => p.status === status)
  )

  // Actions
  async function fetchProducts(params: Partial<ProductSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const query = {
        page: params.page ?? pagination.page,
        size: params.size ?? pagination.size,
        sort: params.sort ?? 'createdAt,desc',
        ...(params.searchTerm && { searchTerm: params.searchTerm }),
        ...(params.status && { status: params.status }),
        ...(params.type && { type: params.type }),
        ...(params.category && { category: params.category })
      }

      const response = await get<ProductListResponse>('/products', { query })
      products.value = response.data.content

      pagination.page = response.data.page
      pagination.size = response.data.size
      pagination.totalElements = response.data.totalElements
      pagination.totalPages = response.data.totalPages
      pagination.first = response.data.first
      pagination.last = response.data.last
      pagination.numberOfElements = response.data.numberOfElements
      pagination.empty = response.data.empty

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch products'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchProductById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<Product>(`/products/${id}`)
      currentProduct.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch product'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createProduct(data: CreateProductCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<Product>('/products', data)

      products.value.unshift(response.data)
      pagination.totalElements++

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to create product'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateProduct(data: UpdateProductCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Product>(`/products/${data.id}`, data)

      const index = products.value.findIndex(p => p.id === data.id)
      if (index !== -1) {
        products.value[index] = response.data
      }

      if (currentProduct.value?.id === data.id) {
        currentProduct.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to update product'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function changeProductStatus(data: ChangeProductStatusCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Product>(`/products/${data.id}/status`, data)

      const index = products.value.findIndex(p => p.id === data.id)
      if (index !== -1) {
        products.value[index] = response.data
      }

      if (currentProduct.value?.id === data.id) {
        currentProduct.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to change product status'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteProduct(id: string, version: number) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { del } = useApi()
      await del(`/products/${id}?version=${version}`)

      const index = products.value.findIndex(p => p.id === id)
      if (index !== -1) {
        products.value.splice(index, 1)
        pagination.totalElements--
      }

      if (currentProduct.value?.id === id) {
        currentProduct.value = null
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to delete product'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function getActiveProducts(params: Partial<ProductSearchParams> = {}) {
    return fetchProducts({ ...params, status: 'ACTIVE' })
  }

  async function searchProducts(searchTerm: string, params: Partial<ProductSearchParams> = {}) {
    return fetchProducts({ ...params, searchTerm })
  }

  function setPage(page: number) {
    pagination.page = page
  }

  function setSize(size: number) {
    pagination.size = size
    pagination.page = 0
  }

  function setSort(sort: string) {
    pagination.page = 0
  }

  function reset() {
    products.value = []
    currentProduct.value = null
    error.value = null
    pagination.page = 0
    pagination.size = 20
    pagination.totalElements = 0
    pagination.totalPages = 0
    pagination.first = true
    pagination.last = false
    pagination.numberOfElements = 0
    pagination.empty = true
  }

  return {
    // State
    products,
    currentProduct,
    loading,
    error,
    pagination,

    // Getters
    productCount,
    activeProducts,
    inactiveProducts,
    deprecatedProducts,
    serviceProducts,
    tariffProducts,
    bundleProducts,
    addonProducts,
    mobileProducts,
    broadbandProducts,
    tvProducts,
    cloudProducts,
    getProductById,
    getProductsByType,
    getProductsByCategory,
    getProductsByStatus,

    // Actions
    fetchProducts,
    fetchProductById,
    createProduct,
    updateProduct,
    changeProductStatus,
    deleteProduct,
    getActiveProducts,
    searchProducts,
    setPage,
    setSize,
    setSort,
    reset
  }
})
