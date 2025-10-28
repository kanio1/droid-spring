<template>
  <div class="addresses-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Addresses</h1>
        <p class="page-subtitle">Manage customer addresses and coverage areas</p>
      </div>
      <div class="page-header__actions">
        <AppButton variant="primary" icon="➕" disabled>
          Add Address (Coming Soon)
        </AppButton>
      </div>
    </div>

    <!-- Coming Soon Notice -->
    <div class="coming-soon-notice">
      <div class="coming-soon__content">
        <div class="coming-soon__icon">📍</div>
        <h2 class="coming-soon__title">Address Management Coming Soon</h2>
        <p class="coming-soon__description">
          We're currently implementing the address management module. This feature will allow you to:
        </p>
        <ul class="coming-soon__features">
          <li>Store and manage customer addresses</li>
          <li>Create and assign coverage nodes to geographical areas</li>
          <li>Link addresses to specific customers</li>
          <li>Visualize coverage areas on maps</li>
          <li>Manage network infrastructure locations</li>
        </ul>
        <div class="coming-soon__actions">
          <NuxtLink to="/customers">
            <AppButton variant="primary">
              Manage Customers
            </AppButton>
          </NuxtLink>
          <NuxtLink to="/">
            <AppButton variant="secondary">
              Back to Dashboard
            </AppButton>
          </NuxtLink>
        </div>
      </div>
    </div>

    <!-- Planned Features Preview -->
    <div class="features-preview">
      <h3>Planned Features</h3>
      <div class="features-grid">
        <div class="feature-card">
          <div class="feature-card__icon">🏠</div>
          <h4>Customer Addresses</h4>
          <p>Store and manage customer residential and business addresses with validation and geocoding support.</p>
        </div>
        
        <div class="feature-card">
          <div class="feature-card__icon">🗺️</div>
          <h4>Coverage Nodes</h4>
          <p>Define logical groups of addresses that will be serviced by specific network infrastructure.</p>
        </div>
        
        <div class="feature-card">
          <div class="feature-card__icon">🌐</div>
          <h4>Network Management</h4>
          <p>Link coverage nodes to network devices and infrastructure for service management.</p>
        </div>
        
        <div class="feature-card">
          <div class="feature-card__icon">📊</div>
          <h4>Coverage Analytics</h4>
          <p>Analyze address coverage, service availability, and network utilization metrics.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// Page meta
definePageMeta({
  title: 'Addresses'
})
</script>

<style scoped>
.addresses-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* Page Header */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-6);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.page-header__content {
  flex: 1;
}

.page-title {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.page-subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.page-header__actions {
  flex-shrink: 0;
}

/* Coming Soon Notice */
.coming-soon-notice {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-12);
  text-align: center;
}

.coming-soon__icon {
  font-size: 4rem;
  margin-bottom: var(--space-4);
  opacity: 0.7;
}

.coming-soon__title {
  margin: 0 0 var(--space-4) 0;
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.coming-soon__description {
  margin: 0 0 var(--space-6) 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.coming-soon__features {
  list-style: none;
  padding: 0;
  margin: 0 0 var(--space-8) 0;
  text-align: left;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
}

.coming-soon__features li {
  padding: var(--space-2) 0;
  color: var(--color-text-primary);
  position: relative;
  padding-left: var(--space-6);
}

.coming-soon__features li:before {
  content: '•';
  color: var(--color-primary);
  font-weight: var(--font-weight-bold);
  position: absolute;
  left: var(--space-2);
}

.coming-soon__actions {
  display: flex;
  gap: var(--space-3);
  justify-content: center;
  flex-wrap: wrap;
}

/* Features Preview */
.features-preview {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.features-preview h3 {
  margin: 0 0 var(--space-6) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  text-align: center;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-4);
}

.feature-card {
  padding: var(--space-6);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  text-align: center;
  transition: all var(--transition-base) var(--transition-timing);
}

.feature-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-primary);
}

.feature-card__icon {
  font-size: 2.5rem;
  margin-bottom: var(--space-3);
  opacity: 0.8;
}

.feature-card h4 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.feature-card p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-relaxed);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-4);
  }
  
  .page-title {
    font-size: var(--font-size-2xl);
  }
  
  .coming-soon-notice {
    padding: var(--space-8);
  }
  
  .coming-soon__actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .features-grid {
    grid-template-columns: 1fr;
  }
  
  .feature-card {
    padding: var(--space-4);
  }
}

/* Tablet adjustments */
@media (min-width: 769px) and (max-width: 1024px) {
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
