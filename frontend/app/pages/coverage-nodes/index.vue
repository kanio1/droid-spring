<template>
  <div class="coverage-nodes-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Coverage Nodes</h1>
        <p class="page-subtitle">Manage network coverage areas and infrastructure</p>
      </div>
      <div class="page-header__actions">
        <AppButton variant="primary" icon="🗺️" disabled>
          Create Coverage Node (Coming Soon)
        </AppButton>
      </div>
    </div>

    <!-- Coming Soon Notice -->
    <div class="coming-soon-notice">
      <div class="coming-soon__content">
        <div class="coming-soon__icon">🗺️</div>
        <h2 class="coming-soon__title">Coverage Nodes Coming Soon</h2>
        <p class="coming-soon__description">
          The coverage nodes module is currently under development. This powerful feature will enable you to:
        </p>
        <ul class="coming-soon__features">
          <li>Create logical groupings of customer addresses</li>
          <li>Assign coverage nodes to network infrastructure</li>
          <li>Manage service availability by geographical area</li>
          <li>Track network device assignments and capacity</li>
          <li>Generate coverage maps and service reports</li>
          <li>Plan network expansion and optimization</li>
        </ul>
        <div class="coming-soon__actions">
          <NuxtLink to="/customers">
            <AppButton variant="primary">
              Manage Customers
            </AppButton>
          </NuxtLink>
          <NuxtLink to="/addresses">
            <AppButton variant="secondary">
              View Addresses
            </AppButton>
          </NuxtLink>
          <NuxtLink to="/">
            <AppButton variant="ghost">
              Back to Dashboard
            </AppButton>
          </NuxtLink>
        </div>
      </div>
    </div>

    <!-- Planned Architecture Preview -->
    <div class="architecture-preview">
      <h3>Coverage Node Architecture</h3>
      <div class="architecture-diagram">
        <div class="diagram-layer">
          <div class="layer-title">Customers</div>
          <div class="layer-items">
            <div class="diagram-item diagram-item--customer">
              <span class="item-icon">👤</span>
              <span class="item-label">Customer 1</span>
            </div>
            <div class="diagram-item diagram-item--customer">
              <span class="item-icon">👤</span>
              <span class="item-label">Customer 2</span>
            </div>
            <div class="diagram-item diagram-item--customer">
              <span class="item-icon">👤</span>
              <span class="item-label">Customer N</span>
            </div>
          </div>
        </div>
        
        <div class="diagram-connector">↓</div>
        
        <div class="diagram-layer">
          <div class="layer-title">Coverage Nodes</div>
          <div class="layer-items">
            <div class="diagram-item diagram-item--node">
              <span class="item-icon">🗺️</span>
              <span class="item-label">Downtown Area</span>
            </div>
            <div class="diagram-item diagram-item--node">
              <span class="item-icon">🗺️</span>
              <span class="item-label">Suburban Zone</span>
            </div>
            <div class="diagram-item diagram-item--node">
              <span class="item-icon">🗺️</span>
              <span class="item-label">Rural Coverage</span>
            </div>
          </div>
        </div>
        
        <div class="diagram-connector">↓</div>
        
        <div class="diagram-layer">
          <div class="layer-title">Network Infrastructure</div>
          <div class="layer-items">
            <div class="diagram-item diagram-item--device">
              <span class="item-icon">📡</span>
              <span class="item-label">Base Station 1</span>
            </div>
            <div class="diagram-item diagram-item--device">
              <span class="item-icon">📡</span>
              <span class="item-label">Base Station 2</span>
            </div>
            <div class="diagram-item diagram-item--device">
              <span class="item-icon">🌐</span>
              <span class="item-label">Core Network</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Key Features List -->
    <div class="features-section">
      <h3>Key Features</h3>
      <div class="features-list">
        <div class="feature-item">
          <div class="feature-item__icon">🎯</div>
          <div class="feature-item__content">
            <h4>Geographic Targeting</h4>
            <p>Define coverage areas using geographic boundaries, addresses, or postal codes.</p>
          </div>
        </div>
        
        <div class="feature-item">
          <div class="feature-item__icon">📊</div>
          <div class="feature-item__content">
            <h4>Capacity Planning</h4>
            <p>Monitor node capacity, utilization metrics, and plan for future growth.</p>
          </div>
        </div>
        
        <div class="feature-item">
          <div class="feature-item__icon">🔧</div>
          <div class="feature-item__content">
            <h4>Infrastructure Management</h4>
            <p>Link coverage nodes to network devices and track maintenance schedules.</p>
          </div>
        </div>
        
        <div class="feature-item">
          <div class="feature-item__icon">📈</div>
          <div class="feature-item__content">
            <h4>Analytics & Reporting</h4>
            <p>Generate detailed reports on coverage, performance, and service availability.</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// Page meta
definePageMeta({
  title: 'Coverage Nodes'
})
</script>

<style scoped>
.coverage-nodes-page {
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
  max-width: 700px;
  margin-left: auto;
  margin-right: auto;
}

.coming-soon__features {
  list-style: none;
  padding: 0;
  margin: 0 0 var(--space-8) 0;
  text-align: left;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
  columns: 2;
  column-gap: var(--space-8);
}

.coming-soon__features li {
  padding: var(--space-2) 0;
  color: var(--color-text-primary);
  position: relative;
  padding-left: var(--space-6);
  break-inside: avoid;
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

/* Architecture Preview */
.architecture-preview {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.architecture-preview h3 {
  margin: 0 0 var(--space-6) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  text-align: center;
}

.architecture-diagram {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
}

.diagram-layer {
  width: 100%;
  max-width: 600px;
}

.layer-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
  text-align: center;
  margin-bottom: var(--space-3);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.layer-items {
  display: flex;
  gap: var(--space-3);
  justify-content: center;
  flex-wrap: wrap;
}

.diagram-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3);
  border-radius: var(--radius-lg);
  min-width: 120px;
  text-align: center;
}

.diagram-item--customer {
  background: var(--color-primary-light);
  border: 1px solid var(--color-primary);
}

.diagram-item--node {
  background: var(--color-accent-light);
  border: 1px solid var(--color-accent);
}

.diagram-item--device {
  background: var(--color-warning-light);
  border: 1px solid var(--color-warning);
}

.item-icon {
  font-size: var(--font-size-xl);
}

.item-label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.diagram-connector {
  font-size: var(--font-size-lg);
  color: var(--color-text-muted);
  font-weight: var(--font-weight-bold);
}

/* Features Section */
.features-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.features-section h3 {
  margin: 0 0 var(--space-6) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  text-align: center;
}

.features-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--space-4);
}

.feature-item {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-alt);
}

.feature-item__icon {
  font-size: var(--font-size-2xl);
  flex-shrink: 0;
}

.feature-item__content h4 {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.feature-item__content p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
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
  
  .coming-soon__features {
    columns: 1;
  }
  
  .layer-items {
    flex-direction: column;
    align-items: center;
  }
  
  .features-list {
    grid-template-columns: 1fr;
  }
  
  .feature-item {
    flex-direction: column;
    text-align: center;
  }
}

/* Tablet adjustments */
@media (min-width: 769px) and (max-width: 1024px) {
  .coming-soon__features {
    columns: 1;
  }
  
  .features-list {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
