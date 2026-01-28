<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  currentPage: number;
  totalPages: number;
}>();

const emit = defineEmits<{
  "update:page": [page: number];
}>();

const canGoPrev = computed(() => props.currentPage > 1);
const canGoNext = computed(() => props.currentPage < props.totalPages);

function goPrev() {
  if (canGoPrev.value) {
    emit("update:page", props.currentPage - 1);
  }
}

function goNext() {
  if (canGoNext.value) {
    emit("update:page", props.currentPage + 1);
  }
}
</script>

<template>
  <nav
    role="navigation"
    aria-label="list pagination"
    class="inline-pagination flex items-center gap-3 text-sm mt-3"
  >
    <button
      type="button"
      :disabled="!canGoPrev"
      class="px-2 py-1 text-link hover:text-link-hover hover:underline disabled:text-gray-400 disabled:no-underline disabled:cursor-default transition-colors"
      @click="goPrev"
    >
      ← Prev
    </button>
    <span class="text-gray-600 dark:text-gray-400">
      {{ currentPage }} / {{ totalPages }}
    </span>
    <button
      type="button"
      :disabled="!canGoNext"
      class="px-2 py-1 text-link hover:text-link-hover hover:underline disabled:text-gray-400 disabled:no-underline disabled:cursor-default transition-colors"
      @click="goNext"
    >
      Next →
    </button>
  </nav>
</template>
