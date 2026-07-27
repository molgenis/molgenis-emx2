<script setup lang="ts">
import { computed } from "vue";
import type { ISectionField } from "../../../../types/types";
import { isOntologyValueObject } from "../../../../../metadata-utils/src/types";

const { field } = defineProps<{
  field: ISectionField;
}>();

const asString = computed((): string => {
  const value: unknown = field.value;
  if (!Array.isArray(value)) {
    return "";
  }
  return value
    .filter(isOntologyValueObject)
    .map((term) => term.label ?? term.name)
    .filter(Boolean)
    .join(", ");
});
</script>

<template>
  <p>{{ asString }}</p>
</template>
