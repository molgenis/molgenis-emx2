<script setup lang="ts">
import type { IColumn } from "../../../metadata-utils/src/types";
defineProps<{
  metadata: IColumn;
  data: number;
}>();
</script>

<template>
  {{ data }}
</template>
