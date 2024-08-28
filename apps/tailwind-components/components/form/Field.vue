<script setup lang="ts">
import type { IColumn } from "../../../metadata-utils/src/types";

defineProps<{
  column: IColumn;
}>();

const error = computed(() => {
  return "";
});
</script>

<template>
  <div class="flex flex-col gap-1">
    <div>
      <span class="capitalize text-title font-bold">{{ column.label }}</span
      ><span class="text-disabled ml-3" v-show="column.required">Required</span>
    </div>
    <div class="text-title" v-if="column.description">
      {{ column.description }}
    </div>
    <div>
      <FormFieldInput
        :type="column.columnType"
        :id="column.id"
      ></FormFieldInput>
    </div>
    <div v-if="error">
      <p class="text-invalid">error</p>
    </div>
  </div>
</template>
