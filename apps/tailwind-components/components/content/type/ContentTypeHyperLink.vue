<script setup lang="ts">
import type { ISectionField } from "~/interfaces/types";

defineProps<{
  field: ISectionField;
}>();
</script>

<template>
  <HyperLink :href="field.value" target="_blank"></HyperLink>
</template>
