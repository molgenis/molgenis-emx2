<script setup>
import BackgroundGradient from "./BackgroundGradient.vue";
</script>

<template>
  <div class="min-h-screen bg-base-gradient relative overflow-x-hidden">
    <BackgroundGradient class="z-10" />
    <div class="z-20 relative">
      <slot name="header"></slot>
      <main>
        <slot></slot>
      </main>
      <slot name="footer"></slot>
    </div>
  </div>
</template>
