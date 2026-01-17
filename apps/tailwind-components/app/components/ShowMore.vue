<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from "vue";
import Button from "./Button.vue";

const props = withDefaults(
    defineProps<{
      lines?: number;
      showLabels?: { more: string; less: string };
    }>(),
    {
      lines: 3,
      showLabels: () => ({ more: "show more", less: "show less" }),
    }
);

const paragraphRef = ref<HTMLElement | null>(null);

const expanded = ref(false);
const showButton = ref(false);
const hydrated = ref(false);

/**
 * CSS vars used by clamp
 */
const paragraphStyle = computed(() => ({
  "--lines": String(props.lines),
}));

/**
 * SSR-only estimated collapsed height
 * 1.2em ≈ typical line-height
 */
const ssrCollapsedStyle = computed(() => ({
  maxHeight: `${props.lines * 1.2}em`,
  overflow: "hidden",
}));

onMounted(async () => {
  hydrated.value = true;

  await nextTick();

  const el = paragraphRef.value;
  if (!el) return;

  /**
   * Measure full (unclamped) height
   */
  const fullHeight = el.scrollHeight;

  await nextTick();

  /**
   * Measure clamped height (now that hydration enabled it)
   */
  const clampedHeight = el.clientHeight;

  showButton.value = fullHeight > clampedHeight;
});

/**
 * Collapse and scroll paragraph into view
 */
async function collapseAndScrollToTop() {
  const el = paragraphRef.value;

  expanded.value = false;
  await nextTick();

  el?.scrollIntoView({
    block: "start",
    behavior: "smooth",
  });
}
</script>

<template>
  <div class="expandable-paragraph">
    <p
        ref="paragraphRef"
        class="paragraph"
        :class="{
        clamped: hydrated && !expanded,
        expanded,
      }"
        :style="[
        paragraphStyle,
        !hydrated && !expanded ? ssrCollapsedStyle : {},
      ]"
        :aria-expanded="expanded"
    >
      <slot />

      <Button
          v-if="expanded"
          type="text"
          class="inline-less"
          @click="collapseAndScrollToTop"
      >
        {{ showLabels.less }}
      </Button>
    </p>

    <div v-if="!expanded && showButton" class="controls">
      <Button type="text" @click="expanded = true">
        {{ showLabels.more }}
      </Button>
    </div>
  </div>
</template>

<style scoped>
.expandable-paragraph {
  width: 100%;
}

.paragraph {
  margin: 0;
  word-break: break-word;
}

/* Clamp applies ONLY after hydration */
.clamped {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: var(--lines);
  overflow: hidden;
}

/* Expanded: natural height */
.expanded {
  display: block;
}

.inline-less {
  margin-left: 6px;
}

.controls {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}
</style>
