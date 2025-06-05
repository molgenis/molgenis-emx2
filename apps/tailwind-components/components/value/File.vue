<template>
  {{ data.filename }}
</template>

<script setup lang="ts">
import type { fileValue, IColumn } from "../../../metadata-utils/src/types";
defineProps<{
  metadata: IColumn;
  data: fileValue;
}>();
</script>
