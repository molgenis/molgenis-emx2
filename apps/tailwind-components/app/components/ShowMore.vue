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

const paragraphStyle = computed(() => ({
  "--lines": String(props.lines),
}));

const ssrCollapsedStyle = computed(() => ({
  maxHeight: `${props.lines * 1.2}em`,
  overflow: "hidden",
}));

onMounted(async () => {
  await nextTick();
  const el = paragraphRef.value;
  if (!el) {
    hydrated.value = false;
    return;
  }
  const fullHeight = el.scrollHeight;
  const clampedHeight = el.clientHeight;
  showButton.value = fullHeight > clampedHeight;
  hydrated.value = true;
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
      :class="{
        clamped: hydrated && !expanded,
        expanded,
      }"
      :style="[paragraphStyle, !hydrated && !expanded ? ssrCollapsedStyle : {}]"
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

    <div v-if="!expanded && (!hydrated || showButton)" class="controls">
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

/* Clamp applies after hydration OR immediately on CSR navigation */
.clamped {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: var(--lines);
  overflow: hidden;
}

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
