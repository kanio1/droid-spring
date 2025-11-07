/**
 * Locale Test Data Factory
 *
 * Factory for generating locale-specific test data
 * Supports multiple languages, RTL layouts, and cultural formats
 *
 * Usage:
 * ```typescript
 * const locale = LocaleFactory.create()
 *   .withLanguage('en')
 *   .withRegion('US')
 *   .withCurrency('USD')
 *   .build()
 * ```
 */

import { faker } from '@faker-js/faker/locale'

export type LanguageCode = 'en' | 'es' | 'fr' | 'de' | 'zh' | 'ja' | 'ar' | 'ru' | 'pt' | 'it' | 'ko' | 'hi'
export type RegionCode = 'US' | 'GB' | 'ES' | 'FR' | 'DE' | 'CN' | 'JP' | 'RU' | 'BR' | 'IT' | 'KR' | 'IN' | 'AU' | 'CA'
export type CurrencyCode = 'USD' | 'EUR' | 'GBP' | 'JPY' | 'CNY' | 'RUB' | 'BRL' | 'INR' | 'AUD' | 'CAD'
export type DateFormat = 'MM/DD/YYYY' | 'DD/MM/YYYY' | 'YYYY-MM-DD' | 'DD.MM.YYYY' | 'YYYY/MM/DD'
export type TimeFormat = '12h' | '24h'
export type LayoutDirection = 'ltr' | 'rtl'

export interface LocaleFactoryOptions {
  language?: LanguageCode
  region?: RegionCode
  currency?: CurrencyCode
  dateFormat?: DateFormat
  timeFormat?: TimeFormat
  direction?: LayoutDirection
  timezone?: string
  firstDayOfWeek?: number // 0 = Sunday, 1 = Monday
  decimalSeparator?: string
  thousandsSeparator?: string
  numberFormat?: string
}

export class LocaleFactory {
  private options: LocaleFactoryOptions = {}

  static create(): LocaleFactory {
    return new LocaleFactory()
  }

  withLanguage(language: LanguageCode): LocaleFactory {
    this.options.language = language
    return this
  }

  withRandomLanguage(): LocaleFactory {
    const languages: LanguageCode[] = ['en', 'es', 'fr', 'de', 'zh', 'ja', 'ar', 'ru', 'pt', 'it', 'ko', 'hi']
    this.options.language = faker.helpers.arrayElement(languages)
    return this
  }

  withRegion(region: RegionCode): LocaleFactory {
    this.options.region = region
    return this
  }

  withRandomRegion(): LocaleFactory {
    const regions: RegionCode[] = ['US', 'GB', 'ES', 'FR', 'DE', 'CN', 'JP', 'RU', 'BR', 'IT', 'KR', 'IN', 'AU', 'CA']
    this.options.region = faker.helpers.arrayElement(regions)
    return this
  }

  withCurrency(currency: CurrencyCode): LocaleFactory {
    this.options.currency = currency
    return this
  }

  withRandomCurrency(): LocaleFactory {
    const currencies: CurrencyCode[] = ['USD', 'EUR', 'GBP', 'JPY', 'CNY', 'RUB', 'BRL', 'INR', 'AUD', 'CAD']
    this.options.currency = faker.helpers.arrayElement(currencies)
    return this
  }

  withDateFormat(format: DateFormat): LocaleFactory {
    this.options.dateFormat = format
    return this
  }

  withTimeFormat(format: TimeFormat): LocaleFactory {
    this.options.timeFormat = format
    return this
  }

  withDirection(direction: LayoutDirection): LocaleFactory {
    this.options.direction = direction
    return this
  }

  rtl(): LocaleFactory {
    this.options.direction = 'rtl'
    return this
  }

  ltr(): LocaleFactory {
    this.options.direction = 'ltr'
    return this
  }

  withTimezone(timezone: string): LocaleFactory {
    this.options.timezone = timezone
    return this
  }

  withRandomTimezone(): LocaleFactory {
    const timezones = [
      'UTC',
      'America/New_York',
      'America/Los_Angeles',
      'Europe/London',
      'Europe/Paris',
      'Europe/Berlin',
      'Asia/Tokyo',
      'Asia/Shanghai',
      'Asia/Seoul',
      'Asia/Kolkata',
      'America/Sao_Paulo',
      'Australia/Sydney'
    ]
    this.options.timezone = faker.helpers.arrayElement(timezones)
    return this
  }

  withFirstDayOfWeek(day: number): LocaleFactory {
    this.options.firstDayOfWeek = day
    return this
  }

  withDecimalSeparator(separator: string): LocaleFactory {
    this.options.decimalSeparator = separator
    return this
  }

  withThousandsSeparator(separator: string): LocaleFactory {
    this.options.thousandsSeparator = separator
    return this
  }

  withNumberFormat(format: string): LocaleFactory {
    this.options.numberFormat = format
    return this
  }

  // Convenience methods for common locales
  englishUS(): LocaleFactory {
    return this
      .withLanguage('en')
      .withRegion('US')
      .withCurrency('USD')
      .withDateFormat('MM/DD/YYYY')
      .withTimeFormat('12h')
      .withDirection('ltr')
      .withTimezone('America/New_York')
      .withFirstDayOfWeek(0)
      .withDecimalSeparator('.')
      .withThousandsSeparator(',')
  }

  spanish(): LocaleFactory {
    return this
      .withLanguage('es')
      .withRegion('ES')
      .withCurrency('EUR')
      .withDateFormat('DD/MM/YYYY')
      .withTimeFormat('24h')
      .withDirection('ltr')
      .withTimezone('Europe/Madrid')
      .withFirstDayOfWeek(1)
      .withDecimalSeparator(',')
      .withThousandsSeparator('.')
  }

  french(): LocaleFactory {
    return this
      .withLanguage('fr')
      .withRegion('FR')
      .withCurrency('EUR')
      .withDateFormat('DD/MM/YYYY')
      .withTimeFormat('24h')
      .withDirection('ltr')
      .withTimezone('Europe/Paris')
      .withFirstDayOfWeek(1)
      .withDecimalSeparator(',')
      .withThousandsSeparator(' ')
  }

  german(): LocaleFactory {
    return this
      .withLanguage('de')
      .withRegion('DE')
      .withCurrency('EUR')
      .withDateFormat('DD.MM.YYYY')
      .withTimeFormat('24h')
      .withDirection('ltr')
      .withTimezone('Europe/Berlin')
      .withFirstDayOfWeek(1)
      .withDecimalSeparator(',')
      .withThousandsSeparator('.')
  }

  chinese(): LocaleFactory {
    return this
      .withLanguage('zh')
      .withRegion('CN')
      .withCurrency('CNY')
      .withDateFormat('YYYY-MM-DD')
      .withTimeFormat('24h')
      .withDirection('ltr')
      .withTimezone('Asia/Shanghai')
      .withFirstDayOfWeek(1)
      .withDecimalSeparator('.')
      .withThousandsSeparator(',')
  }

  japanese(): LocaleFactory {
    return this
      .withLanguage('ja')
      .withRegion('JP')
      .withCurrency('JPY')
      .withDateFormat('YYYY/MM/DD')
      .withTimeFormat('24h')
      .withDirection('ltr')
      .withTimezone('Asia/Tokyo')
      .withFirstDayOfWeek(0)
      .withDecimalSeparator('.')
      .withThousandsSeparator(',')
  }

  arabic(): LocaleFactory {
    return this
      .withLanguage('ar')
      .withRegion('SA')
      .withCurrency('SAR')
      .withDateFormat('DD/MM/YYYY')
      .withTimeFormat('12h')
      .withDirection('rtl')
      .withTimezone('Asia/Riyadh')
      .withFirstDayOfWeek(6)
      .withDecimalSeparator('.')
      .withThousandsSeparator(',')
  }

  russian(): LocaleFactory {
    return this
      .withLanguage('ru')
      .withRegion('RU')
      .withCurrency('RUB')
      .withDateFormat('DD.MM.YYYY')
      .withTimeFormat('24h')
      .withDirection('ltr')
      .withTimezone('Europe/Moscow')
      .withFirstDayOfWeek(1)
      .withDecimalSeparator(',')
      .withThousandsSeparator(' ')
  }

  build(): LocaleConfig {
    // Set defaults if not specified
    if (!this.options.language) {
      this.withRandomLanguage()
    }
    if (!this.options.region) {
      this.withRandomRegion()
    }
    if (!this.options.currency) {
      this.withRandomCurrency()
    }
    if (!this.options.direction) {
      // RTL languages
      if (['ar', 'he', 'fa', 'ur'].includes(this.options.language)) {
        this.options.direction = 'rtl'
      } else {
        this.options.direction = 'ltr'
      }
    }
    if (!this.options.timezone) {
      this.withRandomTimezone()
    }

    return {
      language: this.options.language!,
      region: this.options.region!,
      currency: this.options.currency!,
      dateFormat: this.options.dateFormat || this.getDefaultDateFormat(this.options.language!),
      timeFormat: this.options.timeFormat || this.getDefaultTimeFormat(this.options.region!),
      direction: this.options.direction!,
      timezone: this.options.timezone!,
      firstDayOfWeek: this.options.firstDayOfWeek ?? this.getDefaultFirstDayOfWeek(this.options.region!),
      decimalSeparator: this.options.decimalSeparator || this.getDefaultDecimalSeparator(this.options.region!),
      thousandsSeparator: this.options.thousandsSeparator || this.getDefaultThousandsSeparator(this.options.region!),
      numberFormat: this.options.numberFormat || this.getDefaultNumberFormat(this.options.region!)
    }
  }

  buildMany(count: number): LocaleConfig[] {
    return Array.from({ length: count }, () => this.build())
  }

  private getDefaultDateFormat(language: LanguageCode): DateFormat {
    const formats: Record<LanguageCode, DateFormat> = {
      'en': 'MM/DD/YYYY',
      'es': 'DD/MM/YYYY',
      'fr': 'DD/MM/YYYY',
      'de': 'DD.MM.YYYY',
      'zh': 'YYYY-MM-DD',
      'ja': 'YYYY/MM/DD',
      'ar': 'DD/MM/YYYY',
      'ru': 'DD.MM.YYYY',
      'pt': 'DD/MM/YYYY',
      'it': 'DD/MM/YYYY',
      'ko': 'YYYY-MM-DD',
      'hi': 'DD/MM/YYYY'
    }
    return formats[language] || 'MM/DD/YYYY'
  }

  private getDefaultTimeFormat(region: RegionCode): TimeFormat {
    const twelveHourRegions: RegionCode[] = ['US', 'GB', 'AU', 'CA', 'IN']
    return twelveHourRegions.includes(region) ? '12h' : '24h'
  }

  private getDefaultFirstDayOfWeek(region: RegionCode): number {
    const mondayRegions: RegionCode[] = ['GB', 'ES', 'FR', 'DE', 'CN', 'JP', 'RU', 'IT', 'KR', 'AU']
    return mondayRegions.includes(region) ? 1 : 0
  }

  private getDefaultDecimalSeparator(region: RegionCode): string {
    const commaRegions: RegionCode[] = ['GB', 'ES', 'FR', 'DE', 'RU', 'IT']
    return commaRegions.includes(region) ? ',' : '.'
  }

  private getDefaultThousandsSeparator(region: RegionCode): string {
    const dotRegions: RegionCode[] = ['GB', 'DE']
    const spaceRegions: RegionCode[] = ['FR', 'RU', 'IT']
    if (dotRegions.includes(region)) return '.'
    if (spaceRegions.includes(region)) return ' '
    return ','
  }

  private getDefaultNumberFormat(region: RegionCode): string {
    // Basic number format pattern
    const formats: Record<RegionCode, string> = {
      'US': '#,##0.00',
      'GB': '#,##0.00',
      'ES': '#.##0,00',
      'FR': '# ##0,00',
      'DE': '#.##0,00',
      'CN': '#,##0.00',
      'JP': '#,##0',
      'RU': '# ##0,00',
      'BR': '#.##0,00',
      'IT': '#.##0,00',
      'KR': '#,##0',
      'IN': '#,##,##0.00',
      'AU': '#,##0.00',
      'CA': '#,##0.00'
    }
    return formats[region] || '#,##0.00'
  }
}

/**
 * Locale configuration interface
 */
export interface LocaleConfig {
  language: LanguageCode
  region: RegionCode
  currency: CurrencyCode
  dateFormat: DateFormat
  timeFormat: TimeFormat
  direction: LayoutDirection
  timezone: string
  firstDayOfWeek: number
  decimalSeparator: string
  thousandsSeparator: string
  numberFormat: string
}

/**
 * Predefined locale profiles
 */
export class LocaleProfiles {
  static get englishUS(): LocaleConfig {
    return LocaleFactory.create().englishUS().build()
  }

  static get spanish(): LocaleConfig {
    return LocaleFactory.create().spanish().build()
  }

  static get french(): LocaleConfig {
    return LocaleFactory.create().french().build()
  }

  static get german(): LocaleConfig {
    return LocaleFactory.create().german().build()
  }

  static get chinese(): LocaleConfig {
    return LocaleFactory.create().chinese().build()
  }

  static get japanese(): LocaleConfig {
    return LocaleFactory.create().japanese().build()
  }

  static get arabic(): LocaleConfig {
    return LocaleFactory.create().arabic().build()
  }

  static get russian(): LocaleConfig {
    return LocaleFactory.create().russian().build()
  }

  static get allLocales(): LocaleConfig[] {
    return [
      this.englishUS,
      this.spanish,
      this.french,
      this.german,
      this.chinese,
      this.japanese,
      this.arabic,
      this.russian
    ]
  }
}
