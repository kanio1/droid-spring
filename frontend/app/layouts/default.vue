<template>
  <div class="app">
    <header class="app__header">
      <h1 class="app__title">
        BSS Portal
      </h1>
      <nav class="app__nav" aria-label="Primary navigation">
        <NuxtLink to="/" class="app__link" exact-active-class="app__link--active">
          Session
        </NuxtLink>
        <NuxtLink to="/hello-world" class="app__link" exact-active-class="app__link--active">
          Hello world
        </NuxtLink>
      </nav>
    </header>
    <main class="app__main">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #0f172a;
  color: #f1f5f9;
  font-family: 'Inter', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.app__header {
  padding: 1.5rem 2rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
}

.app__title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
}

.app__nav {
  display: flex;
  gap: 1rem;
}

.app__link {
  color: rgba(226, 232, 240, 0.8);
  text-decoration: none;
  font-weight: 500;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.app__link:hover {
  color: #f8fafc;
  background: rgba(148, 163, 184, 0.18);
}

.app__link--active {
  color: #f8fafc;
  background: rgba(59, 130, 246, 0.25);
}

.app__main {
  flex: 1;
  padding: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
