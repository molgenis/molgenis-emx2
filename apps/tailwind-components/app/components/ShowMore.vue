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
    collapse?: boolean;
    /** The caller is holding back content that is not in the slot yet. */
    hasMore?: boolean;
  }>(),
  {
    maxLines: 3,
    lineStep: 5,
    collapse: true,
  }
);

const emit = defineEmits<{ showMore: [] }>();

const lines = ref(props.maxLines);
const target = ref<HTMLElement | null>(null);
const control = ref<HTMLElement | null>(null);
const overflows = ref(false);
const measured = ref(false);

watch(
  () => props.maxLines,
  (value) => (lines.value = value)
);

const isClamped = computed(() => props.collapse);

// Clamped until measurement says otherwise, because the server cannot measure: an
// unclamped first paint sends the whole text and then snaps to the bound. Clamping
// short content changes nothing, so this is only ever right.
const isCut = computed(
  () => isClamped.value && (overflows.value || !measured.value)
);

const isExpanded = computed(
  () =>
    props.maxLines != null &&
    lines.value != null &&
    lines.value > props.maxLines
);

// Never during render: reading scrollHeight forces a synchronous reflow. Measures
// in the bounded configuration, which is not the one on screen while nothing is
// cut, because line-clamp discards its overflow instead of overflowing.
function measure() {
  const element = target.value;
  if (!element || !props.collapse) return;
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
  // A slot can put any label in the control, so clear what it actually covers.
  const width = control.value?.offsetWidth;
  if (width) {
    element.style.setProperty("--show-more-clear", `${width}px`);
  }
}

let sizeObserver: ResizeObserver | undefined;
let contentObserver: MutationObserver | undefined;

// Only a clamped block can hide anything. A table page holds hundreds of
// unclamped ones, and none of them should cost an observer.
function startObserving() {
  if (sizeObserver || typeof ResizeObserver === "undefined") return;
  if (!isClamped.value || !target.value) return;
  sizeObserver = new ResizeObserver(measure);
  sizeObserver.observe(target.value);
  // A cut block keeps its height when its values change, so the ResizeObserver
  // never fires. Attributes stay unobserved: measure() writes its own styles.
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

// The mask clears the control's width, so its arrival is a reason to re-measure.
watch(canShowMore, () => nextTick(measure));

// Never alongside `show more`: two controls side by side read as one confused one.
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
  <span :class="{ 'show-more-root': isClamped }">
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
/*
 * Prefixed line-clamp only: the standard `line-clamp` shorthand sets
 * `continue: discard`, which wants a block container, and setting both leaves the
 * element clamped by neither.
 */
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

/* The mask erases the tail of the last line, ellipsis and all, so the control needs
   no paint: the surface is not a colour, two themes back the page with a gradient.
   Layer one fades that line's right end, layer two keeps everything above whole. */
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
