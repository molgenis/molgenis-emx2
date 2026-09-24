<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
import ShowMore from "../ShowMore.vue";
defineProps<{
  metadata: IColumn;
  data?: string | null;
  maxLines?: number;
  truncate?: boolean;
}>();
</script>

<template>
  <ShowMore v-if="data" :maxLines="maxLines" :truncate="truncate !== false">{{
    data
  }}</ShowMore>
</template>
