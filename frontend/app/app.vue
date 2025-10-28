<template>
  <NuxtLayout>
    <NuxtPage 
      :transition="{
        name: 'page',
        mode: 'out-in',
        duration: 200
      }" 
    />
  </NuxtLayout>
</template>

<style>
@import './assets/styles/tokens.css';
@import './assets/styles/base.css';
@import './assets/styles/transitions.css';
</style>
