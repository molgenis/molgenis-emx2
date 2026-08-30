<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    showSideNav?: boolean;
  }>(),
  {
    showSideNav: true,
  }
);

const slots = defineSlots<{
  header?: () => any;
  sidebar?: () => any;
  main?: () => any;
}>();

const hasSidebar = computed(() => props.showSideNav && !!slots.sidebar);
</script>

<template>
  <div class="detail-page-layout">
    <header v-if="$slots.header" class="w-full">
      <slot name="header"></slot>
    </header>
    <div class="md:flex md:items-start">
      <aside
        v-if="hasSidebar"
        class="md:w-82.5 md:sticky md:top-7.5 flex-shrink-0"
      >
        <slot name="sidebar"></slot>
      </aside>
      <main :class="{ 'md:pl-7.5': hasSidebar }" class="grow min-w-0">
        <slot name="main"></slot>
      </main>
    </div>
  </div>
</template>
