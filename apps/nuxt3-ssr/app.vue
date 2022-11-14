<template>
  <!-- Markup shared across all pages, ex: NavBar -->
  <div class="min-h-screen bg-base-gradient">
    <slot name="header">
      <AppHeader />
    </slot>
    <main>
      <slot>
        <NuxtPage />
      </slot>
    </main>
    <slot name="footer"></slot>
  </div>
</template>

<script>
import '@/assets/css/main.css'
</script>
