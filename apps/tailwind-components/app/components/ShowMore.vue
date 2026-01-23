<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from "vue";
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
const measured = ref(false);
const overflows = ref(false);

const paragraphStyle = computed(() => ({
  "--lines": String(props.lines),
}));

const showButton = computed(() => !measured.value || overflows.value);

function measureOverflow() {
  const el = paragraphRef.value;
  if (!el) return;

  const { maxHeight, overflow } = el.style;
  el.style.maxHeight = "none";
  el.style.overflow = "visible";
  const natural = el.scrollHeight;
  el.style.maxHeight = maxHeight;
  el.style.overflow = overflow;

  const constrained =
    parseFloat(getComputedStyle(el as unknown as Element).maxHeight) ||
    el.clientHeight;
  overflows.value = natural > constrained + 1;
  measured.value = true;
}

let resizeObserver: ResizeObserver | null = null;

onMounted(async () => {
  await nextTick();
  measureOverflow();
  const el = paragraphRef.value;
  if (el) {
    resizeObserver = new ResizeObserver(measureOverflow);
    resizeObserver.observe(el as unknown as Element);
  }
});

onUnmounted(() => {
  resizeObserver?.disconnect();
});

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
      :class="{ collapsed: !expanded, faded: !expanded && showButton }"
      :style="paragraphStyle"
      :aria-expanded="expanded"
    >
      <slot />
    </p>
    <div v-if="!expanded && showButton" class="button-container">
      <Button type="text" size="tiny" @click="expanded = true">
        {{ showLabels.more }}
      </Button>
    </div>
    <div v-if="expanded" class="button-container">
      <Button type="text" size="tiny" @click="collapseAndScrollToTop">
        {{ showLabels.less }}
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

.collapsed {
  max-height: calc(var(--lines) * 1lh);
  overflow: hidden;
}

.collapsed.faded {
  -webkit-mask-image: linear-gradient(to bottom, black 50%, transparent);
  mask-image: linear-gradient(to bottom, black 50%, transparent);
}

.button-container {
  margin-top: 0.25em;
}
</style>
