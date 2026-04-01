<template>
  <nav
    class="flex items-center gap-2 text-xs text-definition-list-term"
    aria-label="Pagination"
  >
    <button
      type="button"
      class="px-2 py-1 rounded border border-color-theme bg-content text-title disabled:opacity-50 disabled:cursor-not-allowed hover:bg-hover"
      :disabled="safeCurrentPage <= 1"
      @click="goToPage(1)"
    >
      First
    </button>
    <button
      type="button"
      class="px-2 py-1 rounded border border-color-theme bg-content text-title disabled:opacity-50 disabled:cursor-not-allowed hover:bg-hover"
      :disabled="safeCurrentPage <= 1"
      @click="goToPage(safeCurrentPage - 1)"
    >
      Prev
    </button>
    <span class="px-1 tabular-nums">
      Page {{ safeCurrentPage }} of {{ safeTotalPages }}
    </span>
    <button
      type="button"
      class="px-2 py-1 rounded border border-color-theme bg-content text-title disabled:opacity-50 disabled:cursor-not-allowed hover:bg-hover"
      :disabled="safeCurrentPage >= safeTotalPages"
      @click="goToPage(safeCurrentPage + 1)"
    >
      Next
    </button>
    <button
      type="button"
      class="px-2 py-1 rounded border border-color-theme bg-content text-title disabled:opacity-50 disabled:cursor-not-allowed hover:bg-hover"
      :disabled="safeCurrentPage >= safeTotalPages"
      @click="goToPage(safeTotalPages)"
    >
      Last
    </button>
  </nav>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
	currentPage: number;
	totalPages: number;
}>();

const emit = defineEmits<(e: "update", page: number) => void>();

const safeTotalPages = computed(() => {
	const parsed = Number(props.totalPages);
	if (!Number.isFinite(parsed)) return 1;
	return Math.max(1, Math.floor(parsed));
});

const safeCurrentPage = computed(() => {
	const parsed = Number(props.currentPage);
	if (!Number.isFinite(parsed)) return 1;
	return Math.min(safeTotalPages.value, Math.max(1, Math.floor(parsed)));
});

function goToPage(page: number) {
	const target = Math.min(safeTotalPages.value, Math.max(1, Math.floor(page)));
	if (target !== safeCurrentPage.value) {
		emit("update", target);
	}
}
</script>
