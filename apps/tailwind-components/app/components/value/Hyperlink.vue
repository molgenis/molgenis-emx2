<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
import BaseIcon from "../BaseIcon.vue";
defineProps<{
  metadata: IColumn;
  data?: string | null;
}>();
</script>

<template>
  <!-- break-words, not nowrap: a URL is one unbroken token and overran every
  container that does not clip. A table cell still shows one line, because
  CellEMX2 wraps the value in .truncate and white-space is inherited. -->
  <span class="break-words text-link">
    <BaseIcon name="external-link" class="inline mr-2" />
    <a class="underline" target="_blank" :href="data ?? undefined">{{
      data
    }}</a>
  </span>
</template>
