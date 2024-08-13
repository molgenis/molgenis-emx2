<template>
  <p>{{ asString }}</p>
</template>

<script setup lang="ts">
import type { ISectionField } from '~/types/types';

const { field } = defineProps<{
  field: ISectionField;
}>();

const asString = computed(() => {
  return field.value?.map((item:any): any => item.name).join(", ");
});
</script>
