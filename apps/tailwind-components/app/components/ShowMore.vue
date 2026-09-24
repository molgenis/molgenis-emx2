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
    truncate?: boolean;
    hasMore?: boolean;
  }>(),
  {
    maxLines: 3,
    lineStep: 5,
    truncate: true,
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

const isClamped = computed(() => props.truncate);

// Clamp before the client measures. The server cannot measure, so an unclamped
// first paint would snap to the bound as soon as the page hydrates.
const isCut = computed(
  () => isClamped.value && (overflows.value || !measured.value)
);

const isExpanded = computed(
  () =>
    props.maxLines != null &&
    lines.value != null &&
    lines.value > props.maxLines
);

// Do not call this during render. Reading scrollHeight forces a synchronous reflow.
function measureOverflow() {
  const element = target.value;
  if (!element || !props.truncate) return;
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
  // Take the line height from the measurement rather than a named token. A
  // hyperlink's own line-height is 26px where the token says 24px.
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
  sizeObserver = new ResizeObserver(measureOverflow);
  // Observe the root, not the box. A ResizeObserver ignores inline elements, and
  // an uncut box is inline, so watching the box would mean narrowing the window
  // never re-clamps.
  sizeObserver.observe(root.value ?? target.value);
  // A clamped block keeps its height when its content changes, so a resize
  // observer never fires for it; watch content changes directly instead. Skip
  // attribute changes, or measureOverflow()'s own inline styles would retrigger it.
  contentObserver = new MutationObserver(measureOverflow);
  contentObserver.observe(target.value, {
    childList: true,
    subtree: true,
    characterData: true,
  });
  measureOverflow();
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

watch(canShowMore, () => nextTick(measureOverflow));

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
  nextTick(measureOverflow);
}

function showLess() {
  lines.value = props.maxLines;
  nextTick(measureOverflow);
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
/* Use the prefixed properties only. The standard line-clamp shorthand wants a
   block container, and setting both leaves the element clamped by neither. */
.show-more-root {
  position: relative;
  display: inline-block;
  max-width: 100%;
}

/* Allow a break anywhere. The separator is a non-breaking space and a UUID is
   one long word, so without this the list runs as one line off the page. */
.show-more-box {
  overflow-wrap: anywhere;
}

/* Erase the tail of the last line, ellipsis and all, so the control needs no
   paint of its own. Two themes back the page with a gradient, so no flat
   colour would match it. */
.show-more {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: var(--show-more-lines);
  overflow: hidden;
}

/* Apply the fade only once measured. --show-more-line comes from
   measureOverflow(), and an unset value makes mask-size invalid, which fades
   every line instead of the last one. */
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

/* Inherit the text's line-height, or a smaller control would sit off the last
   line's baseline. */
.show-more-over {
  position: absolute;
  right: 0;
  bottom: 0;
  margin-left: 0;
  padding-left: 1em;
  line-height: inherit;
}
</style>
