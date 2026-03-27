<script setup lang="ts">
import { computed } from "vue";
import type { fileValue, IColumn } from "../../../../metadata-utils/src/types";

const props = defineProps<{
  metadata: IColumn;
  data?: fileValue | null;
}>();

const fileName = computed(() => props.data?.filename ?? "No file");

const formattedSize = computed(() => {
  const size = props.data?.size;
  if (typeof size !== "number" || Number.isNaN(size) || size < 0) {
    return "Unknown size";
  }

  if (size === 0) {
    return "0 B";
  }

  const units = ["bytes", "KB", "MB", "GB", "TB"];
  const unitIndex = Math.min(
    Math.floor(Math.log(size) / Math.log(1024)),
    units.length - 1
  );
  const value = size / 1024 ** unitIndex;

  return `${value.toFixed(value >= 10 || unitIndex === 0 ? 0 : 1)} ${
    units[unitIndex]
  }`;
});

const downloadUrl = computed(() => props.data?.url || "");
</script>

<template>
  <span class="overflow-ellipsis whitespace-nowrap overflow-hidden flex-0">
    <a
      v-if="downloadUrl"
      :href="downloadUrl"
      :download="fileName"
      class="text-link underline"
    >
      {{ fileName }}
    </a>
    <span v-else class="truncate">{{ fileName }}</span>

    <span> (size: {{ formattedSize }})</span>
  </span>
</template>
