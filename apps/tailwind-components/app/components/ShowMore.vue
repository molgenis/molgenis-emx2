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
    /** Set false where the caller bounds the content itself, such as a table cell. */
    collapsible?: boolean;
    /** The caller is holding back content that is not in the slot yet. */
    hasMore?: boolean;
  }>(),
  {
    maxLines: 3,
    lineStep: 5,
    collapsible: true,
  }
);

const emit = defineEmits<{ showMore: [] }>();

const lines = ref(props.maxLines);
const root = ref<HTMLElement | null>(null);
const target = ref<HTMLElement | null>(null);
const control = ref<HTMLElement | null>(null);
const overflows = ref(false);
const measured = ref(false);

watch(
  () => props.maxLines,
  (value) => (lines.value = value)
);

const isClamped = computed(() => props.collapsible);

// Clamped until measured: the server cannot measure, so an unclamped first paint
// would snap to the bound on hydration.
const isCut = computed(
  () => isClamped.value && (overflows.value || !measured.value)
);

const isExpanded = computed(
  () =>
    props.maxLines != null &&
    lines.value != null &&
    lines.value > props.maxLines
);

// Never during render: reading scrollHeight forces a synchronous reflow.
function measure() {
  const element = target.value;
  if (!element || !props.collapsible) return;
  const saved = element.style.cssText;
  element.style.display = "-webkit-box";
  element.style.webkitBoxOrient = "vertical";
  element.style.overflow = "hidden";
  element.style.webkitLineClamp = String(lines.value);
  const bounded = element.clientHeight;
  element.style.webkitLineClamp = "unset";
  const full = element.scrollHeight;
  element.style.cssText = saved;
  overflows.value = full > bounded + 1;
  measured.value = true;
  // Measured, not named: a hyperlink's own line-height runs 26px against 24px.
  element.style.setProperty("--show-more-line", `${bounded / lines.value}px`);
  const width = control.value?.offsetWidth;
  if (width) {
    element.style.setProperty("--show-more-clear", `${width}px`);
  }
}

let sizeObserver: ResizeObserver | undefined;
let contentObserver: MutationObserver | undefined;

function startObserving() {
  if (sizeObserver || typeof ResizeObserver === "undefined") return;
  if (!isClamped.value || !target.value) return;
  sizeObserver = new ResizeObserver(measure);
  // The root, not the box: a ResizeObserver ignores a non-replaced inline element,
  // and an uncut box is inline, so narrowing the window would never re-clamp it.
  sizeObserver.observe(root.value ?? target.value);
  // A cut block keeps its height when its values change, so resize never fires.
  // Attributes stay unobserved, or measure()'s own styles would wake it.
  contentObserver = new MutationObserver(measure);
  contentObserver.observe(target.value, {
    childList: true,
    subtree: true,
    characterData: true,
  });
  measure();
}

function stopObserving() {
  sizeObserver?.disconnect();
  sizeObserver = undefined;
  contentObserver?.disconnect();
  contentObserver = undefined;
  overflows.value = false;
  measured.value = false;
}

onMounted(startObserving);
onBeforeUnmount(stopObserving);

watch(isClamped, (clamped) =>
  clamped ? nextTick(startObserving) : stopObserving()
);

const canShowMore = computed(
  () => isClamped.value && (overflows.value || props.hasMore)
);

watch(canShowMore, () => nextTick(measure));

const canShowLess = computed(
  () => isExpanded.value && !overflows.value && !props.hasMore
);

const clampStyle = computed(() =>
  isClamped.value ? { "--show-more-lines": String(lines.value) } : undefined
);

function showMore() {
  if (overflows.value) {
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
  <span ref="root" :class="{ 'show-more-root': isClamped }">
    <span
      ref="target"
      class="show-more-box"
      :class="{ 'show-more': isCut, 'show-more-faded': overflows }"
      :style="clampStyle"
    >
      <slot />
    </span>
    <button
      v-if="canShowMore"
      ref="control"
      class="show-more-control text-link text-body-sm"
      :class="{ 'show-more-over': isCut }"
      :aria-expanded="isExpanded"
      @click="showMore"
    >
      <slot name="more" :hasMore="hasMore">show more</slot>
    </button>
    <button
      v-if="canShowLess"
      class="show-more-control text-link text-body-sm"
      :aria-expanded="true"
      @click="showLess"
    >
      <slot name="less">show less</slot>
    </button>
  </span>
</template>

<style scoped>
/* Prefixed only: the standard `line-clamp` shorthand wants a block container, and
   setting both leaves the element clamped by neither. */
.show-more-root {
  position: relative;
  display: inline-block;
  max-width: 100%;
}

/* Values offer no break of their own: the separator is a non-breaking space and a
   UUID is one long word. Without this the list is one line off the page. */
.show-more-box {
  overflow-wrap: anywhere;
}

/* Erases the tail of the last line, ellipsis and all, so the control needs no paint:
   two themes back the page with a gradient, so no colour would match. */
.show-more {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: var(--show-more-lines);
  overflow: hidden;
}

/* Only once measured: --show-more-line comes from measure(), and an unset one
   makes mask-size invalid, which fades every line instead of the last. */
.show-more-faded {
  -webkit-mask-image: var(--show-more-mask);
  mask-image: var(--show-more-mask);
  -webkit-mask-size: var(--show-more-mask-size);
  mask-size: var(--show-more-mask-size);
  -webkit-mask-position: left bottom, left top;
  mask-position: left bottom, left top;
  -webkit-mask-repeat: no-repeat;
  mask-repeat: no-repeat;

  --show-more-mask: linear-gradient(
      to right,
      #000 calc(100% - var(--show-more-clear, 6em) - 4em),
      transparent calc(100% - var(--show-more-clear, 6em))
    ),
    linear-gradient(#000, #000);
  --show-more-mask-size: 100% var(--show-more-line),
    100% calc(100% - var(--show-more-line));
}

.show-more-control {
  margin-left: 0.5em;
}

/* line-height is the text's, or a smaller control sits off the last line's baseline. */
.show-more-over {
  position: absolute;
  right: 0;
  bottom: 0;
  margin-left: 0;
  padding-left: 1em;
  line-height: inherit;
}
</style>
