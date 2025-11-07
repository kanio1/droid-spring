/**
 * Tenant Test Data Factory
 *
 * Object Mother pattern for generating multi-tenant test data
 *
 * Usage:
 * ```typescript
 * const tenant = TenantFactory.create()
 *   .withName('ACME Corp')
 *   .withCode('acme')
 *   .active()
 *   .enterprise()
 *   .build()
 * ```
 */

import { faker } from '@faker-js/faker'
import type { Tenant } from '../../../app/schemas/tenant'

export type TenantStatus = 'active' | 'inactive' | 'suspended' | 'deleted' | 'trial' | 'expired'
export type TenantTier = 'basic' | 'premium' | 'enterprise'
export type UserTenantRole = 'owner' | 'admin' | 'manager' | 'support' | 'user' | 'viewer' | 'auditor'

export interface TenantFactoryOptions {
  name?: string
  code?: string
  domain?: string
  contactEmail?: string
  phone?: string
  address?: string
  city?: string
  state?: string
  postalCode?: string
  country?: string
  status?: TenantStatus
  tier?: TenantTier
  timezone?: string
  locale?: string
  currency?: string
  industry?: string
  maxUsers?: number
  maxCustomers?: number
  storageQuotaMb?: number
  apiRateLimit?: number
  customBranding?: any
  logoUrl?: string
  settings?: TenantSettings
  createdAt?: Date
  updatedAt?: Date
}

export interface TenantSettings {
  allowSelfRegistration?: boolean
  requireEmailVerification?: boolean
  twoFactorRequired?: boolean
  sessionTimeoutMinutes?: number
  passwordPolicyMinLength?: number
  passwordPolicyRequireUppercase?: boolean
  passwordPolicyRequireLowercase?: boolean
  passwordPolicyRequireNumbers?: boolean
  passwordPolicyRequireSymbols?: boolean
  allowApiAccess?: boolean
  apiKeyRequired?: boolean
  webhookEnabled?: boolean
  dataRetentionDays?: number
  enableAuditLog?: boolean
  allowFileUploads?: boolean
  maxFileSizeMb?: number
  allowedFileTypes?: string
  enableRealtimeNotifications?: boolean
  enableSso?: boolean
  ssoProvider?: string
  primaryColor?: string
  secondaryColor?: string
  customCss?: string
  billingCycle?: string
  trialDays?: number
  gracePeriodDays?: number
  featureFlags?: any
  customFieldsConfig?: any
}

export class TenantFactory {
  private options: TenantFactoryOptions = {}

  static create(): TenantFactory {
    return new TenantFactory()
  }

  withName(name: string): TenantFactory {
    this.options.name = name
    return this
  }

  withRandomName(): TenantFactory {
    this.options.name = faker.company.name()
    return this
  }

  withCode(code: string): TenantFactory {
    this.options.code = code
    return this
  }

  withRandomCode(): TenantFactory {
    this.options.code = faker.string.alphanumeric({ length: 8 }).toLowerCase()
    return this
  }

  withDomain(domain: string): TenantFactory {
    this.options.domain = domain
    return this
  }

  withRandomDomain(): TenantFactory {
    const name = (this.options.name || faker.company.name()).toLowerCase().replace(/[^a-z0-9]/g, '')
    this.options.domain = `${name}.${faker.internet.domainTld()}`
    return this
  }

  withContactEmail(email: string): TenantFactory {
    this.options.contactEmail = email
    return this
  }

  withRandomContactEmail(): TenantFactory {
    this.options.contactEmail = faker.internet.email()
    return this
  }

  withPhone(phone: string): TenantFactory {
    this.options.phone = phone
    return this
  }

  withRandomPhone(): TenantFactory {
    this.options.phone = faker.phone.number()
    return this
  }

  withAddress(address: string): TenantFactory {
    this.options.address = address
    return this
  }

  withRandomAddress(): TenantFactory {
    this.options.address = faker.location.streetAddress()
    return this
  }

  withCity(city: string): TenantFactory {
    this.options.city = city
    return this
  }

  withRandomCity(): TenantFactory {
    this.options.city = faker.location.city()
    return this
  }

  withState(state: string): TenantFactory {
    this.options.state = state
    return this
  }

  withRandomState(): TenantFactory {
    this.options.state = faker.location.state()
    return this
  }

  withPostalCode(postalCode: string): TenantFactory {
    this.options.postalCode = postalCode
    return this
  }

  withRandomPostalCode(): TenantFactory {
    this.options.postalCode = faker.location.zipCode()
    return this
  }

  withCountry(country: string): TenantFactory {
    this.options.country = country
    return this
  }

  withRandomCountry(): TenantFactory {
    this.options.country = faker.location.country()
    return this
  }

  active(): TenantFactory {
    this.options.status = 'active'
    return this
  }

  inactive(): TenantFactory {
    this.options.status = 'inactive'
    return this
  }

  suspended(): TenantFactory {
    this.options.status = 'suspended'
    return this
  }

  trial(): TenantFactory {
    this.options.status = 'trial'
    return this
  }

  expired(): TenantFactory {
    this.options.status = 'expired'
    return this
  }

  basic(): TenantFactory {
    this.options.tier = 'basic'
    this.options.maxUsers = 10
    this.options.maxCustomers = 100
    this.options.storageQuotaMb = 1024 // 1GB
    this.options.apiRateLimit = 100
    return this
  }

  premium(): TenantFactory {
    this.options.tier = 'premium'
    this.options.maxUsers = 50
    this.options.maxCustomers = 1000
    this.options.storageQuotaMb = 10240 // 10GB
    this.options.apiRateLimit = 1000
    return this
  }

  enterprise(): TenantFactory {
    this.options.tier = 'enterprise'
    this.options.maxUsers = 500
    this.options.maxCustomers = 10000
    this.options.storageQuotaMb = 102400 // 100GB
    this.options.apiRateLimit = 10000
    this.options.settings = {
      allowSelfRegistration: true,
      requireEmailVerification: true,
      twoFactorRequired: true,
      sessionTimeoutMinutes: 120,
      passwordPolicyMinLength: 12,
      allowApiAccess: true,
      apiKeyRequired: true,
      webhookEnabled: true,
      dataRetentionDays: 1095, // 3 years
      enableAuditLog: true,
      allowFileUploads: true,
      maxFileSizeMb: 50,
      enableRealtimeNotifications: true,
      billingCycle: 'annually'
    }
    return this
  }

  withTimezone(timezone: string): TenantFactory {
    this.options.timezone = timezone
    return this
  }

  withRandomTimezone(): TenantFactory {
    const timezones = [
      'UTC',
      'America/New_York',
      'America/Los_Angeles',
      'Europe/London',
      'Europe/Paris',
      'Asia/Tokyo',
      'Asia/Shanghai',
      'Australia/Sydney'
    ]
    this.options.timezone = faker.helpers.arrayElement(timezones)
    return this
  }

  withLocale(locale: string): TenantFactory {
    this.options.locale = locale
    return this
  }

  withRandomLocale(): TenantFactory {
    const locales = ['en', 'es', 'fr', 'de', 'zh', 'ja', 'ar', 'ru']
    this.options.locale = faker.helpers.arrayElement(locales)
    return this
  }

  withCurrency(currency: string): TenantFactory {
    this.options.currency = currency
    return this
  }

  withRandomCurrency(): TenantFactory {
    const currencies = ['USD', 'EUR', 'GBP', 'JPY', 'CNY', 'AUD', 'CAD']
    this.options.currency = faker.helpers.arrayElement(currencies)
    return this
  }

  withIndustry(industry: string): TenantFactory {
    this.options.industry = industry
    return this
  }

  withRandomIndustry(): TenantFactory {
    const industries = [
      'Technology',
      'Healthcare',
      'Finance',
      'Education',
      'Retail',
      'Manufacturing',
      'Real Estate',
      'Telecommunications'
    ]
    this.options.industry = faker.helpers.arrayElement(industries)
    return this
  }

  withMaxUsers(maxUsers: number): TenantFactory {
    this.options.maxUsers = maxUsers
    return this
  }

  withMaxCustomers(maxCustomers: number): TenantFactory {
    this.options.maxCustomers = maxCustomers
    return this
  }

  withStorageQuotaMb(storageQuotaMb: number): TenantFactory {
    this.options.storageQuotaMb = storageQuotaMb
    return this
  }

  withApiRateLimit(apiRateLimit: number): TenantFactory {
    this.options.apiRateLimit = apiRateLimit
    return this
  }

  withCustomBranding(branding: any): TenantFactory {
    this.options.customBranding = branding
    return this
  }

  withLogoUrl(logoUrl: string): TenantFactory {
    this.options.logoUrl = logoUrl
    return this
  }

  withRandomLogoUrl(): TenantFactory {
    this.options.logoUrl = faker.image.urlPicsumPhotos()
    return this
  }

  withSettings(settings: TenantSettings): TenantFactory {
    this.options.settings = settings
    return this
  }

  withCreatedAt(date: Date): TenantFactory {
    this.options.createdAt = date
    return this
  }

  withUpdatedAt(date: Date): TenantFactory {
    this.options.updatedAt = date
    return this
  }

  build(): Tenant {
    const now = new Date()

    // Ensure name and code are set
    if (!this.options.name) {
      this.withRandomName()
    }
    if (!this.options.code) {
      const name = (this.options.name || '').toLowerCase().replace(/[^a-z0-9]/g, '')
      this.options.code = name.substring(0, 8)
    }
    if (!this.options.domain) {
      this.withRandomDomain()
    }
    if (!this.options.contactEmail) {
      this.withRandomContactEmail()
    }
    if (!this.options.phone) {
      this.withRandomPhone()
    }
    if (!this.options.timezone) {
      this.withRandomTimezone()
    }
    if (!this.options.locale) {
      this.withRandomLocale()
    }
    if (!this.options.currency) {
      this.withRandomCurrency()
    }
    if (!this.options.industry) {
      this.withRandomIndustry()
    }

    return {
      id: faker.string.uuid(),
      name: this.options.name,
      code: this.options.code,
      domain: this.options.domain,
      contactEmail: this.options.contactEmail,
      phone: this.options.phone,
      address: this.options.address,
      city: this.options.city,
      state: this.options.state,
      postalCode: this.options.postalCode,
      country: this.options.country,
      status: this.options.status || 'active',
      customBranding: this.options.customBranding,
      logoUrl: this.options.logoUrl,
      timezone: this.options.timezone,
      locale: this.options.locale,
      currency: this.options.currency,
      industry: this.options.industry,
      tenantTier: this.options.tier || 'basic',
      maxUsers: this.options.maxUsers,
      maxCustomers: this.options.maxCustomers,
      storageQuotaMb: this.options.storageQuotaMb,
      apiRateLimit: this.options.apiRateLimit,
      settings: this.options.settings,
      createdAt: this.options.createdAt?.toISOString() || now.toISOString(),
      updatedAt: this.options.updatedAt?.toISOString() || now.toISOString()
    }
  }

  buildMany(count: number): Tenant[] {
    return Array.from({ length: count }, () => this.build())
  }
}

/**
 * Predefined tenant profiles for common test scenarios
 */
export class TenantProfiles {
  static get basicTrial(): Tenant {
    return TenantFactory.create()
      .withRandomName()
      .withRandomCode()
      .trial()
      .basic()
      .build()
  }

  static get activeBasic(): Tenant {
    return TenantFactory.create()
      .withRandomName()
      .withRandomCode()
      .active()
      .basic()
      .build()
  }

  static get activePremium(): Tenant {
    return TenantFactory.create()
      .withRandomName()
      .withRandomCode()
      .active()
      .premium()
      .build()
  }

  static get activeEnterprise(): Tenant {
    return TenantFactory.create()
      .withRandomName()
      .withRandomCode()
      .active()
      .enterprise()
      .build()
  }

  static get suspendedTenant(): Tenant {
    return TenantFactory.create()
      .withRandomName()
      .withRandomCode()
      .suspended()
      .basic()
      .build()
  }

  static get multiTenantCompany(): Tenant {
    return TenantFactory.create()
      .withName('MultiCorp Solutions')
      .withCode('multicorp')
      .withDomain('multicorp.example.com')
      .withContactEmail('admin@multicorp.example.com')
      .active()
      .enterprise()
      .withMaxUsers(1000)
      .withMaxCustomers(50000)
      .withSettings({
        allowSelfRegistration: true,
        requireEmailVerification: true,
        twoFactorRequired: true,
        enableRealtimeNotifications: true,
        billingCycle: 'annually'
      })
      .build()
  }

  static get startupTenant(): Tenant {
    return TenantFactory.create()
      .withRandomName()
      .withRandomCode()
      .trial()
      .basic()
      .withMaxUsers(5)
      .withMaxCustomers(50)
      .withSettings({
        allowSelfRegistration: true,
        requireEmailVerification: false,
        dataRetentionDays: 90,
        trialDays: 30
      })
      .build()
  }

  static get governmentTenant(): Tenant {
    return TenantFactory.create()
      .withName('Government Agency')
      .withCode('gov')
      .withDomain('gov.example.com')
      .withContactEmail('security@gov.example.com')
      .active()
      .enterprise()
      .withMaxUsers(10000)
      .withMaxCustomers(1000000)
      .withSettings({
        allowSelfRegistration: false,
        requireEmailVerification: true,
        twoFactorRequired: true,
        enableAuditLog: true,
        dataRetentionDays: 2555, // 7 years
        allowFileUploads: true,
        maxFileSizeMb: 100,
        billingCycle: 'annually'
      })
      .build()
  }
}
