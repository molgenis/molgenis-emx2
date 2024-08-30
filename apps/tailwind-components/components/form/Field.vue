<script setup lang="ts">
import type {
  columnValue,
  IColumn,
  IFieldError,
} from "../../../metadata-utils/src/types";

const props = defineProps<{
  column: IColumn;
  data: columnValue;
  errors: IFieldError[];
}>();

defineEmits(["error", "update:modelValue"]);

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
        :label="column.label"
        :data="data"
        :required="!!column.required"
        @focus="touched = true"
        @input="pristine = false"
        @update:modelValue="$emit('update:modelValue', $event)"
        @error="$emit('error', $event)"
      ></FormFieldInput>
      <div
        v-if="hasError"
        class="bg-yellow-200 text-required p-3 font-bold flex items-center gap-1"
      >
        <BaseIcon name="info"></BaseIcon>{{ errors[0].message }}
      </div>
    </div>
  </div>
</template>
