<script setup lang="ts">
import { ISectionField, IOntologyNode } from "~/interfaces/types";
const { field } = defineProps<{
  field: ISectionField;
}>();

const asString = computed(() => {
  return field.value?.map((item): any => item.name).join(", ");
});
</script>

<template>
  <p>{{ asString }}</p>
</template>
