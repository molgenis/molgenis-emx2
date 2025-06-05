<script setup lang="ts">
import type { IColumn } from "../../../metadata-utils/src/types";
defineProps<{
  metadata: IColumn;
  data: boolean | null | undefined;
}>();
</script>

<template>
  {{ data }}
</template>
