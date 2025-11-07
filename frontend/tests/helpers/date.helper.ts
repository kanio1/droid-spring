/**
 * Date and Time Helper Utilities
 *
 * Provides utilities for working with dates in tests
 * - Date formatting
 * - Date comparisons
 * - Time zone handling
 */

export class DateHelper {
  /**
   * Format date to ISO string
   */
  static formatISO(date: Date): string {
    return date.toISOString()
  }

  /**
   * Format date to readable string
   */
  static formatReadable(date: Date, locale: string = 'en-US'): string {
    return date.toLocaleDateString(locale, {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
  }

  /**
   * Add days to date
   */
  static addDays(date: Date, days: number): Date {
    const result = new Date(date)
    result.setDate(result.getDate() + days)
    return result
  }

  /**
   * Add months to date
   */
  static addMonths(date: Date, months: number): Date {
    const result = new Date(date)
    result.setMonth(result.getMonth() + months)
    return result
  }

  /**
   * Add years to date
   */
  static addYears(date: Date, years: number): Date {
    const result = new Date(date)
    result.setFullYear(result.getFullYear() + years)
    return result
  }

  /**
   * Get relative date string
   */
  static getRelativeDate(date: Date): string {
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

    if (diffDays === 0) {
      return 'Today'
    } else if (diffDays === 1) {
      return 'Yesterday'
    } else if (diffDays > 0 && diffDays < 7) {
      return `${diffDays} days ago`
    } else if (diffDays > 0 && diffDays < 30) {
      const weeks = Math.floor(diffDays / 7)
      return `${weeks} ${weeks === 1 ? 'week' : 'weeks'} ago`
    } else if (diffDays > 0) {
      const months = Math.floor(diffDays / 30)
      return `${months} ${months === 1 ? 'month' : 'months'} ago`
    } else {
      const futureDays = Math.abs(diffDays)
      if (futureDays === 1) {
        return 'Tomorrow'
      } else if (futureDays < 7) {
        return `In ${futureDays} days`
      } else if (futureDays < 30) {
        const weeks = Math.floor(futureDays / 7)
        return `In ${weeks} ${weeks === 1 ? 'week' : 'weeks'}`
      } else {
        const months = Math.floor(futureDays / 30)
        return `In ${months} ${months === 1 ? 'month' : 'months'}`
      }
    }
  }

  /**
   * Get date in specific timezone
   */
  static getDateInTimezone(date: Date, timezone: string): string {
    return new Intl.DateTimeFormat('en-US', {
      timeZone: timezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }).format(date)
  }

  /**
   * Check if date is today
   */
  static isToday(date: Date): boolean {
    const today = new Date()
    return (
      date.getDate() === today.getDate() &&
      date.getMonth() === today.getMonth() &&
      date.getFullYear() === today.getFullYear()
    )
  }

  /**
   * Check if date is within range
   */
  static isWithinRange(
    date: Date,
    startDate: Date,
    endDate: Date
  ): boolean {
    return date >= startDate && date <= endDate
  }

  /**
   * Get start of day
   */
  static startOfDay(date: Date): Date {
    const result = new Date(date)
    result.setHours(0, 0, 0, 0)
    return result
  }

  /**
   * Get end of day
   */
  static endOfDay(date: Date): Date {
    const result = new Date(date)
    result.setHours(23, 59, 59, 999)
    return result
  }

  /**
   * Get start of month
   */
  static startOfMonth(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), 1)
  }

  /**
   * Get end of month
   */
  static endOfMonth(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth() + 1, 0, 23, 59, 59, 999)
  }

  /**
   * Generate random date within range
   */
  static randomDateInRange(startDate: Date, endDate: Date): Date {
    const start = startDate.getTime()
    const end = endDate.getTime()
    const randomTime = start + Math.random() * (end - start)
    return new Date(randomTime)
  }

  /**
   * Parse date string
   */
  static parseDate(dateString: string): Date {
    return new Date(dateString)
  }

  /**
   * Get difference in days between two dates
   */
  static daysDifference(date1: Date, date2: Date): number {
    const timeDiff = Math.abs(date2.getTime() - date1.getTime())
    return Math.ceil(timeDiff / (1000 * 60 * 60 * 24))
  }

  /**
   * Get difference in months between two dates
   */
  static monthsDifference(date1: Date, date2: Date): number {
    const yearsDiff = date2.getFullYear() - date1.getFullYear()
    const monthsDiff = date2.getMonth() - date1.getMonth()
    return yearsDiff * 12 + monthsDiff
  }

  /**
   * Check if date is weekend
   */
  static isWeekend(date: Date): boolean {
    const day = date.getDay()
    return day === 0 || day === 6 // Sunday or Saturday
  }

  /**
   * Get business days between two dates
   */
  static getBusinessDaysBetween(startDate: Date, endDate: Date): number {
    let count = 0
    const current = new Date(startDate)

    while (current <= endDate) {
      if (!this.isWeekend(current)) {
        count++
      }
      current.setDate(current.getDate() + 1)
    }

    return count
  }
}
