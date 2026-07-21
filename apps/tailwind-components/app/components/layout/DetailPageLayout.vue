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
    <div class="xl:flex xl:items-start">
      <aside
        v-if="hasSidebar"
        class="xl:w-82.5 sticky top-[30px] flex-shrink-0 hidden xl:block"
      >
        <slot name="sidebar"></slot>
      </aside>
      <main :class="{ 'xl:pl-7.5': hasSidebar }" class="grow min-w-0">
        <slot name="main"></slot>
      </main>
    </div>
  </div>
</template>
