<script setup lang="ts">
import { computed } from "vue";
import type { ISectionField } from "../../../../types/types";
import type { columnValueObject } from "../../../../metadata-utils/src/types";

const { field } = defineProps<{
  field: ISectionField;
}>();

const asString = computed((): string => {
  const items = field.value as columnValueObject[];
  return items?.map((item) => String(item.name ?? "")).join(", ") ?? "";
});
</script>

<template>
  <p>{{ asString }}</p>
</template>
