<script setup lang="ts">
import type { fileValue, IColumn } from "../../../../metadata-utils/src/types";
defineProps<{
  metaData: IColumn;
  data: fileValue;
}>();
</script>

<template>
  {{ data.filename }}
</template>
