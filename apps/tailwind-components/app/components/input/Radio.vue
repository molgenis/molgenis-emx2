<template>
  <input
    :id="id"
    type="radio"
    :value="value"
    :checked="modelValue === value"
    :disabled="disabled"
    class="w-5 h-5 ml-[6px] mr-2.5 mt-0.5 hover:cursor-pointer"
    @focus="$emit('focus')"
    @change="$emit('update:modelValue', value)"
  />
</template>

<script lang="ts" setup>
import type { columnValue } from "../../../../metadata-utils/src/types";
import { type IInputProps } from "../../../types/types";

defineProps<IInputProps & { value: columnValue }>();
defineModel<columnValue>();
defineEmits(["focus", "update:modelValue"]);
</script>
