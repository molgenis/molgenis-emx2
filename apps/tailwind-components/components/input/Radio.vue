<template>
  <input
    :id="id"
    :value="value"
    v-model="modelValue"
    type="radio"
    :disabled="disabled"
    class="w-5 h-5 ml-[6px] mr-2.5 mt-0.5 hover:cursor-pointer"
  />
</template>

<script lang="ts" setup>
import type { columnValue } from "../../../metadata-utils/src/types";
import { type InputProps, InputPropsDefaults } from "~/types/types";
withDefaults(
  defineProps<
    InputProps & {
      value: columnValue;
    }
  >(),
  {
    ...InputPropsDefaults,
  }
);
const modelValue = defineModel<columnValue>();
</script>
