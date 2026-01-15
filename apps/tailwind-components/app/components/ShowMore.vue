<script setup lang="ts">
import { ref, onMounted } from "vue";
import Button from "./Button.vue";

const props = withDefaults(
    defineProps<{
      lines?: number;
      showLabels?: { more: string; less: string };
      ssrHeight?: string; // e.g. '120px' for SSR render
    }>(),
    {
      lines: 3,
      showLabels: () => ({ more: "show more", less: "show less" }),
      ssrHeight: "120px"
    }
);

const expanded = ref(false);
const clientMounted = ref(false);
const showButton = ref(true);
const paragraphRef = ref<HTMLElement | null>(null);

onMounted(() => {
  clientMounted.value = true;

  if (paragraphRef.value) {
    // Check if content overflows
    const el = paragraphRef.value;
    showButton.value = el.scrollHeight > el.clientHeight;
  }
});
</script>

<template>
  <div class="expandable-paragraph">
    <p
        ref="paragraphRef"
        class="paragraph"
        :class="{ clamped: !expanded && clientMounted }"
        :style="!expanded
        ? clientMounted
          ? { WebkitLineClamp: lines }
          : { maxHeight: ssrHeight, overflow: 'hidden' }
        : {}"
        aria-expanded="expanded"
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

    <div class="controls" v-if="!expanded && showButton">
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
  display: -webkit-box;
  -webkit-box-orient: vertical;
}

.clamped {
  overflow: hidden;
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
