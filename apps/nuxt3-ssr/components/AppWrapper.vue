<script setup>
</script>

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
      <slot name="header"></slot>
      <main>
        <slot></slot>
      </main>
      <slot name="footer"></slot>
    </div>
  </div>
</template>
