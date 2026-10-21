<template>
  <InputCheckboxGroup
    v-if="isArray"
    :id="id"
    :options="valueLabels"
    :invalid="invalid"
    :valid="valid"
    :disabled="disabled"
    :describedBy="describedBy"
    v-model="arrayModelValue"
    :showClearButton="!required"
    @blur="emit('blur')"
    @focus="emit('focus')"
  />
  <InputRadioGroup
    v-else
    :id="id"
    :options="valueLabels"
    :invalid="invalid"
    :valid="valid"
    :disabled="disabled"
    :describedBy="describedBy"
    v-model="singleModelValue"
    :showClearButton="!required"
    @blur="emit('blur')"
    @focus="emit('focus')"
  />
</template>

<script lang="ts" setup>
import { computed } from "vue";
import type { IInputProps, IValueLabel } from "../../../types/types";
import type {
  columnValue,
  IInputValue,
} from "../../../../metadata-utils/src/types";
import InputCheckboxGroup from "./CheckboxGroup.vue";
import InputRadioGroup from "./RadioGroup.vue";

const props = withDefaults(
  defineProps<
    IInputProps & {
      values: string[];
      isArray?: boolean;
      required?: boolean;
    }
  >(),
  {
    isArray: false,
    required: false,
  }
);

const modelValue = defineModel<
  IInputValue | columnValue[] | undefined | null
>();
const emit = defineEmits(["blur", "focus"]);

const valueLabels = computed<IValueLabel[]>(() =>
  props.values.map((value) => ({ value }))
);

const arrayModelValue = computed<columnValue[]>({
  get() {
    return Array.isArray(modelValue.value) ? modelValue.value : [];
  },
  set(value) {
    modelValue.value = value;
  },
});

const singleModelValue = computed<IInputValue | null>({
  get() {
    return Array.isArray(modelValue.value) ? null : modelValue.value ?? null;
  },
  set(value) {
    modelValue.value = value;
  },
});
</script>
