<script setup lang="ts">
import type { ISectionField } from "../../../../types/types";

defineProps<{
  field: ISectionField;
}>();
</script>

<template>
  <HyperLink :href="(field.value as string)" target="_blank"></HyperLink>
</template>
