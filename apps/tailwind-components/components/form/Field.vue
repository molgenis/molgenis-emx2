<script setup lang="ts">
import type { columnValue, IColumn } from "../../../metadata-utils/src/types";

const props = defineProps<{
  column: IColumn;
  data: columnValue;
  errors: string[];
}>();

const pristine = ref(true);
const dirty = computed(() => !pristine.value);

const touched = ref(false);
const untouched = computed(() => !touched.value);

const hasError = computed(() => props.errors.length > 0);
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
        :data="data"
        @focus="touched = true"
        @input="pristine = false"
      ></FormFieldInput>
    </div>
    <div v-if="hasError">
      <p class="text-invalid">this field has an error</p>
    </div>
  </div>
</template>
