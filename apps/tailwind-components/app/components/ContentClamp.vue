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
  // The mask erases the tail of the LAST line, so it needs that line's height.
  // A value carrying its own line-height makes this taller than the text's, which
  // is why it is measured rather than named: hyperlinks run 26px against 24px.
  element.style.setProperty(
    "--content-clamp-line",
    `${bounded / lines.value}px`
  );
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
      :class="{ 'content-clamp-over': isCut }"
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
 * line-height is still cut on a line. It forces an ellipsis and gives no way to
 * put a control at the cut, so the control is placed over that spot instead.
 *
 * Prefixed only. The standard `line-clamp` shorthand sets `continue: discard`,
 * which wants a block container, and setting both leaves the element clamped by
 * neither.
 */
.content-clamp-root {
  position: relative;
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

/*
 * The mask erases the tail of the last line, ellipsis and all, so the control has
 * clean ground to stand on and needs no paint of its own. Painting it was tried
 * and cannot work here: the surface is not a colour. Two themes back the page with
 * a gradient — uncan-connect runs #ac3cb4 to #2d1b4e — so the colour behind the
 * control differs by scroll position and by row, and every ancestor between the
 * clamp and the body computes `background-color: transparent`. A named token, a
 * caller-passed token and reading the nearest painted ancestor all put a white
 * slab on a purple page.
 *
 * Two layers: the last line fades out at its right end, everything above it stays
 * whole. --content-clamp-line is that line's measured height.
 */
.content-clamp {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: var(--content-clamp-lines);
  overflow: hidden;
  -webkit-mask-image: var(--content-clamp-mask);
  mask-image: var(--content-clamp-mask);
  -webkit-mask-size: var(--content-clamp-mask-size);
  mask-size: var(--content-clamp-mask-size);
  -webkit-mask-position: left bottom, left top;
  mask-position: left bottom, left top;
  -webkit-mask-repeat: no-repeat;
  mask-repeat: no-repeat;

  --content-clamp-mask: linear-gradient(
      to right,
      #000 calc(100% - 7em),
      transparent calc(100% - 1em)
    ),
    linear-gradient(#000, #000);
  --content-clamp-mask-size: 100% var(--content-clamp-line),
    100% calc(100% - var(--content-clamp-line));
}

/* Nothing is cut, so the control just follows the last value. */
.content-clamp-control {
  margin-left: 0.5em;
}

/*
 * Something is cut, so the control sits at the cut, on ground the mask cleared for
 * it. Its line-height is the text's, or a smaller control would sit off the last
 * line's baseline.
 */
.content-clamp-over {
  position: absolute;
  right: 0;
  bottom: 0;
  margin-left: 0;
  padding-left: 1em;
  line-height: inherit;
}
</style>
