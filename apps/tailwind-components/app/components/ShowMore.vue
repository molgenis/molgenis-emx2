<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from "vue";
import Button from "./Button.vue";

const props = withDefaults(
    defineProps<{
      lines?: number;
      showLabels?: { more: string; less: string };
      ssrHeight?: string;
    }>(),
    {
      lines: 3,
      showLabels: () => ({ more: "show more", less: "show less" }),
      ssrHeight: "120px"
    }
);

const paragraphRef = ref<HTMLElement | null>(null);

const expanded = ref(false);
const showButton = ref(false);
const measured = ref(false);

/**
 * CSS variable for line clamp
 */
const paragraphStyle = computed(() => ({
  "--lines": String(props.lines)
}));

onMounted(async () => {
  await nextTick();
  await document.fonts?.ready;

  const el = paragraphRef.value;
  if (!el) return;

  /**
   * Measure without clamp
   */
  el.classList.remove("clamped");
  const fullHeight = el.scrollHeight;

  /**
   * Reapply clamp
   */
  el.classList.add("clamped");
  const clampedHeight = el.clientHeight;

  showButton.value = fullHeight > clampedHeight;
  measured.value = true;
});
</script>

<template>
  <div class="expandable-paragraph">
    <p
        ref="paragraphRef"
        class="paragraph clamped"
        :class="{ expanded }"
        :style="[
        paragraphStyle,
        !measured && !expanded
          ? { maxHeight: ssrHeight, overflow: 'hidden' }
          : {}
      ]"
        :aria-expanded="expanded"
    >
      <slot />

      <Button
          v-if="expanded"
          type="text"
          class="inline-less"
          @click="expanded = false"
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

/* Default: clamped */
.clamped:not(.expanded) {
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
