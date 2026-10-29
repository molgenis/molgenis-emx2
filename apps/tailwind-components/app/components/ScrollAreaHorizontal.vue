<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";

const props = withDefaults(
  defineProps<{
    fadeWidth?: number;
    fadeColor?: string;
    /** Skip this many pixels at the left, for content pinned there (a sticky column). */
    startInset?: number;
    /** Skip this many pixels at the right, for content pinned there. */
    endInset?: number;
  }>(),
  {
    fadeWidth: 48,
    fadeColor: "var(--background-color-table)",
    startInset: 0,
    endInset: 0,
  }
);

const emit = defineEmits<{ scroll: [event: Event] }>();

const scroller = ref<HTMLElement | null>(null);
const atStart = ref(true);
const atEnd = ref(true);

// Do not call this during render. Reading scrollWidth forces a synchronous reflow.
function measure() {
  const element = scroller.value;
  if (!element) return;
  const furthest = element.scrollWidth - element.clientWidth;
  atStart.value = element.scrollLeft <= 1;
  atEnd.value = furthest <= 1 || element.scrollLeft >= furthest - 1;
}

function handleScroll(event: Event) {
  measure();
  emit("scroll", event);
}

let sizeObserver: ResizeObserver | undefined;

onMounted(() => {
  measure();
  if (typeof ResizeObserver === "undefined" || !scroller.value) return;
  sizeObserver = new ResizeObserver(measure);
  sizeObserver.observe(scroller.value);
  for (const child of Array.from(scroller.value.children)) {
    sizeObserver.observe(child);
  }
});

onBeforeUnmount(() => {
  sizeObserver?.disconnect();
  sizeObserver = undefined;
});

const startStyle = computed(() => ({
  left: `${props.startInset}px`,
  width: `${props.fadeWidth}px`,
  backgroundImage: `linear-gradient(to right, ${props.fadeColor}, transparent)`,
}));

const endStyle = computed(() => ({
  right: `${props.endInset}px`,
  width: `${props.fadeWidth}px`,
  backgroundImage: `linear-gradient(to left, ${props.fadeColor}, transparent)`,
}));

defineExpose({ scrollElement: scroller, measure });
</script>

<template>
  <div class="relative">
    <div
      ref="scroller"
      class="overflow-x-auto overscroll-x-contain"
      @scroll="handleScroll"
    >
      <slot />
    </div>
    <div
      aria-hidden="true"
      class="absolute inset-y-0 z-30 pointer-events-none transition-opacity duration-150 ease-in-out motion-reduce:transition-none"
      :class="atStart ? 'opacity-0' : 'opacity-100'"
      :style="startStyle"
    />
    <div
      aria-hidden="true"
      class="absolute inset-y-0 z-30 pointer-events-none transition-opacity duration-150 ease-in-out motion-reduce:transition-none"
      :class="atEnd ? 'opacity-0' : 'opacity-100'"
      :style="endStyle"
    />
  </div>
</template>
