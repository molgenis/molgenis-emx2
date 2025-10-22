<script setup lang="ts">
import type { ISectionField } from "../../../../types/types";
import ContentReadMore from "../../ContentReadMore.vue";

defineProps<{
  field: ISectionField;
}>();
</script>

<template>
  <ContentReadMore :text="field.value" :cutoff="500" />
</template>
