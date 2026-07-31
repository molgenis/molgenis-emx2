<template>
  <InputCheckboxGroup
    :id="id"
    :options="valueLabels"
    :invalid="invalid"
    :valid="valid"
    :disabled="disabled"
    :describedBy="describedBy"
    v-model="safeModelValue"
    :showClearButton="true"
    @blur="emit('blur')"
    @focus="emit('focus')"
  />
</template>

<script lang="ts" setup>
import { computed } from "vue";
import type { IInputProps, IValueLabel } from "../../../types/types";
import type { columnValue } from "../../../../metadata-utils/src/types";
import InputCheckboxGroup from "./CheckboxGroup.vue";

const props = defineProps<
  IInputProps & {
    values: string[];
  }
>();

const modelValue = defineModel<columnValue[] | undefined | null>();
const emit = defineEmits(["blur", "focus"]);

const valueLabels = computed<IValueLabel[]>(() =>
  props.values.map((value) => ({ value }))
);

const safeModelValue = computed<columnValue[]>({
  get() {
    return Array.isArray(modelValue.value) ? modelValue.value : [];
  },
  set(val) {
    modelValue.value = val;
  },
});
</script>
