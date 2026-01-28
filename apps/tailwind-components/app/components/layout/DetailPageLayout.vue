<script setup lang="ts">
withDefaults(
  defineProps<{
    showSideNav?: boolean;
  }>(),
  {
    showSideNav: true,
  }
);
</script>

<template>
  <div class="detail-page-layout">
    <header v-if="$slots.header" class="w-full">
      <slot name="header"></slot>
    </header>
    <div class="xl:flex xl:items-start">
      <aside
        v-if="showSideNav && $slots.side"
        class="xl:w-82.5 sticky top-[30px] flex-shrink-0 hidden xl:block"
      >
        <slot name="side"></slot>
      </aside>
      <main class="xl:pl-7.5 grow min-w-0">
        <slot name="main"></slot>
      </main>
    </div>
  </div>
</template>
