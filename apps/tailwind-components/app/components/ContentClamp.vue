<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";

const props = withDefaults(
  defineProps<{
    /** Lines shown when collapsed. Leave unset to never clamp. */
    maxLines?: number;
    /** Lines each click adds, so a long block reveals in steps. */
    lineStep?: number;
    moreLabel?: string;
    lessLabel?: string;
  }>(),
  {
    lineStep: 5,
    moreLabel: "more",
    lessLabel: "show less",
  }
);

const lines = ref(props.maxLines);
const target = ref<HTMLElement | null>(null);
const overflows = ref(false);

watch(
  () => props.maxLines,
  (value) => (lines.value = value)
);

/**
 * Clamping is CSS and costs nothing. Measuring is the only expensive part, so it
 * runs inside the observer callback where layout has already settled. Reading
 * scrollHeight during render would force a synchronous reflow instead.
 */
const isClamped = computed(() => lines.value !== undefined);

function measure() {
  const element = target.value;
  if (!element) return;
  overflows.value = element.scrollHeight > element.clientHeight + 1;
}

let observer: ResizeObserver | undefined;

/**
 * Only a clamped block can be hiding anything, so only a clamped block is worth
 * observing. An unclamped one would still cost an observer, and a table page can
 * hold hundreds of these.
 */
function startObserving() {
  if (observer || typeof ResizeObserver === "undefined") return;
  if (!isClamped.value || !target.value) return;
  observer = new ResizeObserver(measure);
  observer.observe(target.value);
  measure();
}

function stopObserving() {
  observer?.disconnect();
  observer = undefined;
  overflows.value = false;
}

onMounted(startObserving);
onBeforeUnmount(stopObserving);

watch(isClamped, (clamped) =>
  clamped ? nextTick(startObserving) : stopObserving()
);

const isExpanded = computed(
  () =>
    props.maxLines !== undefined &&
    lines.value !== undefined &&
    lines.value > props.maxLines
);

/**
 * Offered only when the clamp is really hiding something. Counting characters or
 * items instead offers a control for content that already fits.
 */
const canShowMore = computed(() => isClamped.value && overflows.value);

const clampStyle = computed(() =>
  isClamped.value ? { "--content-clamp-lines": String(lines.value) } : undefined
);

function showMore() {
  if (lines.value !== undefined) lines.value += props.lineStep;
  nextTick(measure);
}

function showLess() {
  lines.value = props.maxLines;
  nextTick(measure);
}
</script>

<template>
  <span>
    <span
      ref="target"
      :class="{ 'content-clamp': isClamped }"
      :style="clampStyle"
    >
      <slot />
    </span>
    <button
      v-if="canShowMore"
      class="text-link text-body-sm ml-1"
      :aria-expanded="isExpanded"
      @click="showMore"
    >
      {{ moreLabel }}
    </button>
    <button
      v-if="isExpanded"
      class="text-link text-body-sm ml-1"
      :aria-expanded="true"
      @click="showLess"
    >
      {{ lessLabel }}
    </button>
  </span>
</template>

<style scoped>
/* Bound by space, not by item or character count. The content stays in the DOM. */
.content-clamp {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: var(--content-clamp-lines);
  line-clamp: var(--content-clamp-lines);
  overflow: hidden;
}
</style>
