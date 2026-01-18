<script setup lang="ts">
import type { ISectionColumn } from "../../../../types/types";
import ContentReadMore from "../../ContentReadMore.vue";

defineProps<{
  field: ISectionColumn;
}>();
</script>

<template>
  <ContentReadMore :text="field.value" :cutoff="500" />
</template>
