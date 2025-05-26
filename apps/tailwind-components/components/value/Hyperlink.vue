<script setup lang="ts">
import type { IColumn } from "../../../metadata-utils/src/types";
defineProps<{
  metaData: IColumn;
  data: string;
}>();
</script>

<template>
  <span
    class="overflow-ellipsis whitespace-nowrap overflow-hidden flex-0 text-link"
  >
    <BaseIcon name="external-link" class="inline mr-2" />
    <a class="underline" target="_blank" :href="data">{{ data }}</a>
  </span>
</template>
