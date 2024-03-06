<script setup lang="ts">
import type { ISectionField } from "~/interfaces/types";
defineProps<{
  field: ISectionField;
}>();
</script>

<template>
  <ContentReadMore :text="field.value" :cutoff="500" />
</template>
