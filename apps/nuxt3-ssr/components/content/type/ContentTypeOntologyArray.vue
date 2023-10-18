<script setup lang="ts">
import { ISectionField } from "interfaces/types";
const { field } = defineProps<{
  field: ISectionField;
}>();

const asSting = computed(() => {
  return field.value?.map((item) => item.name).join(", ");
});
</script>

<template>
  <p>{{ asSting }}</p>
</template>
