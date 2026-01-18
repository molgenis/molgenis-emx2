<script setup lang="ts">
import type { ISectionColumn } from "../../../../types/types";

defineProps<{
  field: ISectionColumn;
}>();
</script>

<template>
  <HyperLink :href="field.value" target="_blank"></HyperLink>
</template>
