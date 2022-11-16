<template>
  <div class="min-h-screen bg-base-gradient relative overflow-x-hidden">
    <BackgroundGradient class="z-10" />
    <div class="z-20 relative">
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
  </div>
</template>

<script>
import '@/assets/css/main.css'
import BackgroundGradient from "./components/BackgroundGradient.vue";
</script>
