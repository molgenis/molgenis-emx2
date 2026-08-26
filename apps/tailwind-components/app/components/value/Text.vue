<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
defineProps<{
  metadata: IColumn;
  data?: string | null;
  /** Lines a collapsed text occupies. Leave unset to never collapse. */
  maxLines?: number;
}>();
</script>

<template>
  <ContentClamp v-if="data" :maxLines="maxLines">{{ data }}</ContentClamp>
</template>
