<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
defineProps<{
  metadata: IColumn;
  data?: string | null;
}>();
</script>

<template>
  <ContentReadMore v-if="data" :text="String(data)" />
</template>
