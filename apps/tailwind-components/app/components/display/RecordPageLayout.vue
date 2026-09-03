<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    showLegend?: boolean;
  }>(),
  {
    showLegend: true,
  }
);

const slots = defineSlots<{
  header?: () => any;
  sidebar?: () => any;
  main?: () => any;
}>();

const hasSidebar = computed(() => props.showLegend && !!slots.sidebar);
</script>

<template>
  <div class="detail-page-layout">
    <header v-if="$slots.header" class="w-full">
      <slot name="header"></slot>
    </header>
    <div class="lg:flex lg:items-start">
      <aside
        v-if="hasSidebar"
        class="lg:w-82.5 lg:sticky lg:top-7.5 flex-shrink-0"
      >
        <slot name="sidebar"></slot>
      </aside>
      <main :class="{ 'lg:pl-7.5': hasSidebar }" class="grow min-w-0">
        <slot name="main"></slot>
      </main>
    </div>
  </div>
</template>
