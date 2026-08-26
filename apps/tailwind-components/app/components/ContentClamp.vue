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
  }>(),
  {
    lineStep: 5,
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

/**
 * Only a block that is really cutting something wears the clamp. While nothing is
 * cut the block stays inline, so the control flows after the last value and the
 * browser puts it on the last line's baseline for us.
 */
const isCut = computed(() => isClamped.value && overflows.value);

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
  if (!element || lines.value === undefined) return;
  // Ask the question in the bounded configuration, which is not the one on screen
  // whenever nothing is cut. -webkit-line-clamp then discards the lines past the
  // bound rather than overflowing them, so scrollHeight equals clientHeight and
  // the usual comparison always answers no: lift the bound to read the full
  // height. No paint falls between these writes, so none of it is visible.
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
  // A cut block does not change size when the values inside it change, so the
  // ResizeObserver alone never re-measures a list whose data was replaced or
  // whose next tranche arrived. Attributes are deliberately not observed: the
  // measurement writes its own inline styles.
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
      class="content-clamp-box"
      :class="{ 'content-clamp': isCut }"
      :style="clampStyle"
    >
      <slot />
    </span>
    <button
      v-if="canShowMore"
      class="content-clamp-control text-link text-body-sm"
      :class="{ 'content-clamp-own-line': isCut }"
      :aria-expanded="isExpanded"
      @click="showMore"
    >
      <slot name="more" :hasMore="hasMore">show more</slot>
    </button>
    <button
      v-if="canShowLess"
      class="content-clamp-control text-link text-body-sm"
      :aria-expanded="true"
      @click="showLess"
    >
      <slot name="less">show less</slot>
    </button>
  </span>
</template>

<style scoped>
/*
 * -webkit-line-clamp counts real line boxes, so a value carrying its own
 * line-height is still cut on a line.
 *
 * Prefixed only. The standard `line-clamp` shorthand sets `continue: discard`,
 * which wants a block container, and setting both leaves the element clamped by
 * neither.
 */
.content-clamp-root {
  display: inline-block;
  max-width: 100%;
}

/*
 * Holds whether anything is cut or not. A list of values carries no break
 * opportunity of its own — the separator is a non-breaking space, and a UUID or
 * an address is one long word — so without this the whole list is a single line
 * running off the side of the page.
 */
.content-clamp-box {
  overflow-wrap: anywhere;
}

.content-clamp {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: var(--content-clamp-lines);
  overflow: hidden;
}

/* Nothing is cut, so the control just follows the last value. */
.content-clamp-control {
  margin-left: 0.5em;
}

/*
 * A cut block runs to the edge, so there is no room after it and the control
 * takes the next line. It sits alone there: a control among values wearing the
 * theme's own link colour is distinguished by nothing a reader can see, and a
 * theme can put the values in any colour the control might have used.
 */
.content-clamp-own-line {
  display: block;
  margin-left: auto;
}
</style>
