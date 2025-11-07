/**
 * Data Correlator Engine
 *
 * Manages entity relationships and ensures data consistency
 * Validates entity chains and prevents orphaned records
 */

import type { Customer } from '../../../app/schemas/customer'
import type { Order, OrderItem } from '../../../app/schemas/order'
import type { Invoice, InvoiceLineItem } from '../../../app/schemas/invoice'
import type { Payment } from '../../../app/schemas/payment'
import type { Subscription } from '../../../app/schemas/subscription'

export interface EntityRelationship {
  from: 'customer' | 'order' | 'invoice' | 'payment' | 'subscription'
  to: 'customer' | 'order' | 'invoice' | 'payment' | 'subscription'
  required: boolean
}

export class DataCorrelator {
  private relationships: Map<string, Set<string>> = new Map()
  private entities: Map<string, any> = new Map()
  private relationshipRules: EntityRelationship[] = [
    { from: 'customer', to: 'order', required: false },
    { from: 'customer', to: 'invoice', required: false },
    { from: 'customer', to: 'payment', required: false },
    { from: 'customer', to: 'subscription', required: false },
    { from: 'order', to: 'invoice', required: false },
    { from: 'invoice', to: 'payment', required: false }
  ]

  /**
   * Register an entity in the correlator
   */
  registerEntity(type: string, entity: any, id?: string): string {
    const entityId = id || entity.id
    if (!entityId) {
      throw new Error(`Entity must have an ID or one must be provided`)
    }

    // Store entity
    this.entities.set(`${type}:${entityId}`, entity)

    // Track relationship
    if (!this.relationships.has(type)) {
      this.relationships.set(type, new Set())
    }

    return entityId
  }

  /**
   * Link two entities with a relationship
   */
  linkEntities(
    fromType: string,
    fromId: string,
    toType: string,
    toId: string,
    required: boolean = false
  ): void {
    const fromKey = `${fromType}:${fromId}`
    const toKey = `${toType}:${toId}`

    // Verify both entities exist
    if (!this.entities.has(fromKey)) {
      throw new Error(`Source entity ${fromKey} not found`)
    }
    if (!this.entities.has(toKey)) {
      throw new Error(`Target entity ${toKey} not found`)
    }

    // Update entity references
    const fromEntity = this.entities.get(fromKey)
    const toEntity = this.entities.get(toKey)

    // Set foreign key references
    this.setForeignKey(fromEntity, toType, toId, toEntity)
    this.setForeignKey(toEntity, fromType, fromId, fromEntity)
  }

  /**
   * Get all entities of a specific type
   */
  getEntities(type: string): any[] {
    const entities: any[] = []
    for (const [key, entity] of this.entities) {
      if (key.startsWith(`${type}:`)) {
        entities.push(entity)
      }
    }
    return entities
  }

  /**
   * Get a specific entity
   */
  getEntity(type: string, id: string): any {
    return this.entities.get(`${type}:${id}`)
  }

  /**
   * Validate all entity relationships
   */
  validateRelationships(): { valid: boolean; errors: string[] } {
    const errors: string[] = []

    for (const [key, entity] of this.entities) {
      const [type, id] = key.split(':')

      // Check required relationships
      for (const rule of this.relationshipRules) {
        if (rule.from === type && rule.required) {
          const relatedId = this.getForeignKey(entity, rule.to)
          if (!relatedId) {
            errors.push(`Required relationship missing: ${key} -> ${rule.to}`)
          }
        }
      }
    }

    return {
      valid: errors.length === 0,
      errors
    }
  }

  /**
   * Get entity statistics
   */
  getStatistics(): Record<string, number> {
    const stats: Record<string, number> = {}

    for (const [key] of this.entities) {
      const type = key.split(':')[0]
      stats[type] = (stats[type] || 0) + 1
    }

    return stats
  }

  /**
   * Clear all entities and relationships
   */
  clear(): void {
    this.entities.clear()
    this.relationships.clear()
  }

  /**
   * Export all entities as a dataset
   */
  exportDataset(): {
    customers: Customer[]
    orders: Order[]
    invoices: Invoice[]
    payments: Payment[]
    subscriptions: Subscription[]
  } {
    return {
      customers: this.getEntities('customer') as Customer[],
      orders: this.getEntities('order') as Order[],
      invoices: this.getEntities('invoice') as Invoice[],
      payments: this.getEntities('payment') as Payment[],
      subscriptions: this.getEntities('subscription') as Subscription[]
    }
  }

  private setForeignKey(entity: any, relatedType: string, relatedId: string, relatedEntity: any): void {
    // Set the foreign key based on the related type
    const foreignKeyMap: Record<string, string> = {
      customer: 'customerId',
      order: 'orderId',
      invoice: 'invoiceId',
      payment: 'paymentId',
      subscription: 'subscriptionId'
    }

    const foreignKey = foreignKeyMap[relatedType]
    if (foreignKey && !entity[foreignKey]) {
      entity[foreignKey] = relatedId
    }

    // Set the reverse reference
    const reverseKeyMap: Record<string, string> = {
      customer: 'customer',
      order: 'order',
      invoice: 'invoice',
      payment: 'payment',
      subscription: 'subscription'
    }

    const reverseKey = reverseKeyMap[relatedType]
    if (reverseKey && !entity[reverseKey]) {
      entity[reverseKey] = relatedEntity
    }
  }

  private getForeignKey(entity: any, relatedType: string): string | null {
    const foreignKeyMap: Record<string, string> = {
      customer: 'customerId',
      order: 'orderId',
      invoice: 'invoiceId',
      payment: 'paymentId',
      subscription: 'subscriptionId'
    }

    const foreignKey = foreignKeyMap[relatedType]
    return entity[foreignKey] || null
  }
}

/**
 * Singleton instance of DataCorrelator
 */
export const dataCorrelator = new DataCorrelator()
