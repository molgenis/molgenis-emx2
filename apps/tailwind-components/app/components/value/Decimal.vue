<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
defineProps<{
  metadata?: IColumn;
  data?: number | null;
  precision?: number;
}>();
</script>

<template>
  <span>{{ precision !== undefined ? data?.toFixed(precision) : data }}</span>
</template>
