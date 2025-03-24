<script setup lang="ts">
import { computed } from "vue";
import type { ISectionField } from "~/types/types";

const { field } = defineProps<{
  field: ISectionField;
}>();

interface Item {
  name: string;
}

const asString = computed((): string => {
  return field.value?.map((item: Item): string => item.name).join(", ");
});
</script>

<template>
  <p>{{ asString }}</p>
</template>
