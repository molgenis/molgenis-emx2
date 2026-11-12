<script setup lang="ts">
import type { ISectionField } from "../../../../types/types";
import ShowMore from "../../ShowMore.vue";

defineProps<{
  field: ISectionField;
}>();
</script>

<template>
  <ShowMore :maxLines="8">{{ field.value }}</ShowMore>
</template>
