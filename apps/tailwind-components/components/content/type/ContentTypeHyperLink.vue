<template>
  <HyperLink :href="field.value" target="_blank"></HyperLink>
</template>

<script setup lang="ts">
import type { ISectionField } from "~/types/types";

defineProps<{
  field: ISectionField;
}>();
</script>
