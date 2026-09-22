<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
defineProps<{
  metadata: IColumn;
  data?: string | null;
}>();
</script>

<template>
  <a class="underline" :href="`mailto:${data}`">{{ data }}</a>
</template>
