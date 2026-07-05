<template>
  <div
    ref="panelRef"
    class="relative shrink-0 w-80 xl:w-96 bg-sidebar-gradient rounded-t-3px rounded-b-theme transition-[margin-left] duration-default motion-reduce:transition-none"
    :class="[
      collapsed ? 'panel-slide-collapsed' : 'panel-expanded',
      { 'cursor-pointer': collapsed },
    ]"
    @click="expandWhenCollapsed"
    @transitionend="onTransitionEnd"
  >
    <div id="filter-sidebar-content" v-show="contentVisible" class="pb-8">
      <slot />
    </div>

    <div class="absolute top-3 right-0 flex flex-col items-center gap-2">
      <button
        type="button"
        class="flex items-center justify-center w-10 h-10 text-search-filter-title hover:text-search-filter-group-toggle transition-colors cursor-pointer focus:outline-none focus-visible:outline"
        :aria-label="collapsed ? 'Show filters' : 'Hide filters'"
        @click.stop="emit('update:collapsed', !collapsed)"
      >
        <BaseIcon
          :name="collapsed ? 'double-arrow-right' : 'double-arrow-left'"
          :width="20"
        />
      </button>
      <span
        v-if="activeFilterCount > 0"
        class="inline-flex items-center justify-center w-6 h-6 rounded-full bg-button-primary text-button-primary text-body-xs font-bold"
      >
        {{ activeFilterCount }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import BaseIcon from "./BaseIcon.vue";

const props = defineProps<{
  collapsed: boolean;
  activeFilterCount: number;
}>();

const emit = defineEmits<{
  "update:collapsed": [value: boolean];
}>();

const panelRef = ref<HTMLElement | null>(null);
const contentVisible = ref(!props.collapsed);

function prefersReducedMotion(): boolean {
  return (
    typeof window !== "undefined" &&
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

function expandWhenCollapsed() {
  if (props.collapsed) {
    emit("update:collapsed", false);
  }
}

watch(
  () => props.collapsed,
  (isCollapsed) => {
    if (!isCollapsed) {
      contentVisible.value = true;
    } else {
      if (prefersReducedMotion()) {
        contentVisible.value = false;
      }
    }
  }
);

function onTransitionEnd(event: TransitionEvent) {
  if (event.propertyName !== "margin-left") return;
  if (props.collapsed) {
    contentVisible.value = false;
  }
}

defineExpose({ onTransitionEnd });
</script>

<style scoped>
.panel-expanded {
  margin-left: 0;
}

/* Collapsed slide offset = panel-width − rail-width, per breakpoint */
/* Base/lg (<xl): w-80 (20rem) − rail w-10 (2.5rem) = 17.5rem */
.panel-slide-collapsed {
  margin-left: -17.5rem;
}

/* xl (≥1280): w-96 (24rem) − rail w-10 (2.5rem) = 21.5rem */
@media (min-width: 1280px) {
  .panel-slide-collapsed {
    margin-left: -21.5rem;
  }
}
</style>
