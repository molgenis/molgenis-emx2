<script setup lang="ts">
import { computed } from "vue";
import type { ISectionColumn } from "../../../../types/types";

const { field } = defineProps<{
  field: ISectionColumn;
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
