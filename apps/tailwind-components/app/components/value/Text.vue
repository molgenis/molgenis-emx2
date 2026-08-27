<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
defineProps<{
  metadata: IColumn;
  data?: string | null;
  /** Lines a collapsed text occupies. */
  maxLines?: number;
  /** Set false where the caller bounds the value itself, such as a table cell. */
  collapsible?: boolean;
}>();
</script>

<template>
  <ShowMore
    v-if="data"
    :maxLines="maxLines"
    :collapsible="collapsible !== false"
    >{{ data }}</ShowMore
  >
</template>
