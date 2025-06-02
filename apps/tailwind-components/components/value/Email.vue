<script setup lang="ts">
import type { IColumn } from "../../../metadata-utils/src/types";
defineProps<{
  metadata: IColumn;
  data: string;
}>();
</script>

<template>
  <a class="underline" :href="`mailto:${data}`">{{ data }}</a>
</template>
