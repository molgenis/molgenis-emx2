<script setup lang="ts">
import type { IColumn } from "../../../metadata-utils/src/types";
defineProps<{
  metaData: IColumn;
  data: number;
}>();
</script>

<template>
  {{ data }}
</template>
