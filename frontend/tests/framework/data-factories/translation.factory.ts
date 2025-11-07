/**
 * Translation Test Data Factory
 *
 * Factory for generating translation test data
 * Supports multiple languages with proper translations
 *
 * Usage:
 * ```typescript
 * const translation = TranslationFactory.create()
 *   .withKey('customer.name')
 *   .withTranslations({
 *     en: 'Customer Name',
 *     es: 'Nombre del Cliente'
 *   })
 *   .build()
 * ```
 */

import type { LanguageCode } from './locale.factory'

export interface TranslationEntry {
  key: string
  translations: Record<LanguageCode, string>
  description?: string
  context?: string
  pluralForm?: boolean
}

export class TranslationFactory {
  private entry: Partial<TranslationEntry> = {}

  static create(): TranslationFactory {
    return new TranslationFactory()
  }

  withKey(key: string): TranslationFactory {
    this.entry.key = key
    return this
  }

  withTranslation(language: LanguageCode, translation: string): TranslationFactory {
    if (!this.entry.translations) {
      this.entry.translations = {} as Record<LanguageCode, string>
    }
    this.entry.translations![language] = translation
    return this
  }

  withTranslations(translations: Record<LanguageCode, string>): TranslationFactory {
    this.entry.translations = { ...translations }
    return this
  }

  withDescription(description: string): TranslationFactory {
    this.entry.description = description
    return this
  }

  withContext(context: string): TranslationFactory {
    this.entry.context = context
    return this
  }

  withPluralForm(enable: boolean = true): TranslationFactory {
    this.entry.pluralForm = enable
    return this
  }

  build(): TranslationEntry {
    if (!this.entry.key) {
      throw new Error('Translation key is required')
    }
    if (!this.entry.translations || Object.keys(this.entry.translations).length === 0) {
      throw new Error('At least one translation is required')
    }

    return {
      key: this.entry.key,
      translations: this.entry.translations,
      description: this.entry.description,
      context: this.entry.context,
      pluralForm: this.entry.pluralForm
    }
  }
}

/**
 * Common UI translations
 */
export class UITranslations {
  static get login(): TranslationEntry[] {
    return [
      TranslationFactory.create()
        .withKey('auth.login.title')
        .withTranslations({
          en: 'Login',
          es: 'Iniciar Sesión',
          fr: 'Connexion',
          de: 'Anmelden',
          zh: '登录',
          ja: 'ログイン',
          ar: 'تسجيل الدخول',
          ru: 'Вход'
        })
        .build(),
      TranslationFactory.create()
        .withKey('auth.email')
        .withTranslations({
          en: 'Email',
          es: 'Correo Electrónico',
          fr: 'Email',
          de: 'E-Mail',
          zh: '电子邮件',
          ja: 'メール',
          ar: 'البريد الإلكتروني',
          ru: 'Электронная почта'
        })
        .build(),
      TranslationFactory.create()
        .withKey('auth.password')
        .withTranslations({
          en: 'Password',
          es: 'Contraseña',
          fr: 'Mot de Passe',
          de: 'Passwort',
          zh: '密码',
          ja: 'パスワード',
          ar: 'كلمة المرور',
          ru: 'Пароль'
        })
        .build()
    ]
  }

  static get customer(): TranslationEntry[] {
    return [
      TranslationFactory.create()
        .withKey('customer.title')
        .withTranslations({
          en: 'Customer',
          es: 'Cliente',
          fr: 'Client',
          de: 'Kunde',
          zh: '客户',
          ja: '顧客',
          ar: 'عميل',
          ru: 'Клиент'
        })
        .build(),
      TranslationFactory.create()
        .withKey('customer.name')
        .withTranslations({
          en: 'Name',
          es: 'Nombre',
          fr: 'Nom',
          de: 'Name',
          zh: '姓名',
          ja: '名前',
          ar: 'الاسم',
          ru: 'Имя'
        })
        .build(),
      TranslationFactory.create()
        .withKey('customer.email')
        .withTranslations({
          en: 'Email Address',
          es: 'Dirección de Correo',
          fr: 'Adresse Email',
          de: 'E-Mail-Adresse',
          zh: '电子邮件地址',
          ja: 'メールアドレス',
          ar: 'عنوان البريد الإلكتروني',
          ru: 'Адрес электронной почты'
        })
        .build()
    ]
  }

  static get order(): TranslationEntry[] {
    return [
      TranslationFactory.create()
        .withKey('order.title')
        .withTranslations({
          en: 'Order',
          es: 'Pedido',
          fr: 'Commande',
          de: 'Bestellung',
          zh: '订单',
          ja: '注文',
          ar: 'طلب',
          ru: 'Заказ'
        })
        .build(),
      TranslationFactory.create()
        .withKey('order.status')
        .withTranslations({
          en: 'Status',
          es: 'Estado',
          fr: 'Statut',
          de: 'Status',
          zh: '状态',
          ja: 'ステータス',
          ar: 'الحالة',
          ru: 'Статус'
        })
        .build()
    ]
  }

  static get invoice(): TranslationEntry[] {
    return [
      TranslationFactory.create()
        .withKey('invoice.title')
        .withTranslations({
          en: 'Invoice',
          es: 'Factura',
          fr: 'Facture',
          de: 'Rechnung',
          zh: '发票',
          ja: '請求書',
          ar: 'فاتورة',
          ru: 'Счет'
        })
        .build(),
      TranslationFactory.create()
        .withKey('invoice.total')
        .withTranslations({
          en: 'Total',
          es: 'Total',
          fr: 'Total',
          de: 'Gesamt',
          zh: '总计',
          ja: '合計',
          ar: 'الإجمالي',
          ru: 'Итого'
        })
        .build()
    ]
  }

  static get common(): TranslationEntry[] {
    return [
      TranslationFactory.create()
        .withKey('common.save')
        .withTranslations({
          en: 'Save',
          es: 'Guardar',
          fr: 'Enregistrer',
          de: 'Speichern',
          zh: '保存',
          ja: '保存',
          ar: 'حفظ',
          ru: 'Сохранить'
        })
        .build(),
      TranslationFactory.create()
        .withKey('common.cancel')
        .withTranslations({
          en: 'Cancel',
          es: 'Cancelar',
          fr: 'Annuler',
          de: 'Abbrechen',
          zh: '取消',
          ja: 'キャンセル',
          ar: 'إلغاء',
          ru: 'Отмена'
        })
        .build(),
      TranslationFactory.create()
        .withKey('common.delete')
        .withTranslations({
          en: 'Delete',
          es: 'Eliminar',
          fr: 'Supprimer',
          de: 'Löschen',
          zh: '删除',
          ja: '削除',
          ar: 'حذف',
          ru: 'Удалить'
        })
        .build()
    ]
  }

  static getAll(): TranslationEntry[] {
    return [
      ...this.login,
      ...this.customer,
      ...this.order,
      ...this.invoice,
      ...this.common
    ]
  }
}

/**
 * Translation validation utility
 */
export class TranslationValidator {
  static validateCompleteness(translations: TranslationEntry[], requiredLanguages: LanguageCode[]): ValidationResult {
    const missing: { key: string; language: LanguageCode }[] = []
    const invalid: { key: string; language: LanguageCode; value: string }[] = []

    for (const entry of translations) {
      for (const language of requiredLanguages) {
        const translation = entry.translations[language]

        if (!translation || translation.trim() === '') {
          missing.push({ key: entry.key, language })
        } else if (translation.length > 100) {
          invalid.push({ key: entry.key, language, value: translation })
        }
      }
    }

    return {
      isValid: missing.length === 0 && invalid.length === 0,
      missing,
      invalid
    }
  }

  static validateConsistency(translations: TranslationEntry[]): ValidationResult {
    const duplicates: { key: string; language: LanguageCode; values: string[] }[] = []

    // Group by key
    const byKey = new Map<string, TranslationEntry[]>()
    for (const entry of translations) {
      if (!byKey.has(entry.key)) {
        byKey.set(entry.key, [])
      }
      byKey.get(entry.key)!.push(entry)
    }

    // Check for duplicates
    for (const [key, entries] of byKey) {
      const allTranslations = new Map<LanguageCode, string[]>()
      for (const entry of entries) {
        for (const [lang, value] of Object.entries(entry.translations)) {
          const language = lang as LanguageCode
          if (!allTranslations.has(language)) {
            allTranslations.set(language, [])
          }
          allTranslations.get(language)!.push(value)
        }
      }

      for (const [language, values] of allTranslations) {
        const uniqueValues = [...new Set(values)]
        if (uniqueValues.length > 1) {
          duplicates.push({ key, language, values: uniqueValues })
        }
      }
    }

    return {
      isValid: duplicates.length === 0,
      duplicates
    }
  }
}

interface ValidationResult {
  isValid: boolean
  missing?: { key: string; language: LanguageCode }[]
  invalid?: { key: string; language: LanguageCode; value: string }[]
  duplicates?: { key: string; language: LanguageCode; values: string[] }[]
}
