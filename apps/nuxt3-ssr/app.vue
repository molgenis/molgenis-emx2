<template>
  <div
    class="
      min-h-screen
      bg-base-gradient
      relative
      overflow-x-hidden
      after:bg-black
      after:w-full
      after:h-[166px]
      after:top-0
      after:absolute
      after:opacity-20
      after:z-20
      xl:after:hidden
    "
  >
    <BackgroundGradient class="z-10" />
    <div class="z-30 relative">
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
import "@/assets/css/main.css";
import BackgroundGradient from "./components/BackgroundGradient.vue";
</script>
