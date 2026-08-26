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
    maxLines?: number;
    lineStep?: number;
    /** The caller is holding back content that is not in the slot yet. */
    hasMore?: boolean;
    moreLabel?: string;
    lessLabel?: string;
  }>(),
  {
    lineStep: 5,
    moreLabel: "show more",
    lessLabel: "show less",
  }
);

const emit = defineEmits<{ showMore: [] }>();

const lines = ref(props.maxLines);
const target = ref<HTMLElement | null>(null);
const overflows = ref(false);

watch(
  () => props.maxLines,
  (value) => (lines.value = value)
);

const isClamped = computed(() => lines.value !== undefined);

const isExpanded = computed(
  () =>
    props.maxLines !== undefined &&
    lines.value !== undefined &&
    lines.value > props.maxLines
);

// Inside the observer callback, where layout has already settled. Reading
// scrollHeight during render would force a synchronous reflow.
function measure() {
  const element = target.value;
  if (!element) return;
  overflows.value = element.scrollHeight > element.clientHeight + 1;
}

let observer: ResizeObserver | undefined;

// Only a clamped block can hide anything. A table page holds hundreds of
// unclamped ones, and none of them should cost an observer.
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

const canShowMore = computed(
  () => isClamped.value && (overflows.value || props.hasMore)
);

// Never alongside `show more`: two controls side by side read as one confused one.
const canShowLess = computed(
  () => isExpanded.value && !overflows.value && !props.hasMore
);

const clampStyle = computed(() =>
  isClamped.value ? { "--content-clamp-lines": String(lines.value) } : undefined
);

// One or the other, never both. While the slot still hides something, growing the
// bound is the whole job. Only once it does not is the caller the one who can help.
function showMore() {
  if (overflows.value && lines.value !== undefined) {
    lines.value += props.lineStep;
  } else {
    emit("showMore");
  }
  nextTick(measure);
}

function showLess() {
  lines.value = props.maxLines;
  nextTick(measure);
}
</script>

<template>
  <span :class="{ 'content-clamp-root': isClamped }">
    <span
      ref="target"
      :class="{ 'content-clamp': isClamped }"
      :style="clampStyle"
    >
      <slot />
    </span>
    <button
      v-if="canShowMore"
      class="content-clamp-control text-link text-body-sm"
      :aria-expanded="isExpanded"
      @click="showMore"
    >
      {{ moreLabel }}
    </button>
    <button
      v-if="canShowLess"
      class="content-clamp-control text-link text-body-sm"
      :aria-expanded="true"
      @click="showLess"
    >
      {{ lessLabel }}
    </button>
  </span>
</template>

<style scoped>
/*
 * -webkit-line-clamp counts real line boxes, so a value carrying its own
 * line-height is still cut on a line. It forces an ellipsis and gives no way to
 * put a control at the cut, so the control is placed over that spot instead.
 */
.content-clamp-root {
  position: relative;
  display: inline-block;
  max-width: 100%;
}

.content-clamp {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: var(--content-clamp-lines);
  line-clamp: var(--content-clamp-lines);
  overflow: hidden;
  overflow-wrap: anywhere;
}

/*
 * Sits where the ellipsis is, fading the text out behind it. A surface whose
 * background is not the default sets --content-clamp-bg on any ancestor.
 */
.content-clamp-control {
  position: absolute;
  right: 0;
  bottom: 0;
  padding-left: 2em;
  background: linear-gradient(
    to right,
    transparent,
    var(--content-clamp-bg, var(--color-white)) 1.5em
  );
}
</style>
