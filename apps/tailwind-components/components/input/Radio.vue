<template>
  <input
    :id="id"
    v-model="modelValue"
    type="radio"
    :disabled="disabled"
    class="w-5 h-5 ml-[6px] mr-2.5 mt-0.5 hover:cursor-pointer"
    @focus="$emit('focus')"
  />
</template>

<script lang="ts" setup>
import type { columnValue } from "../../../metadata-utils/src/types";
import { type IInputProps } from "../../types/types";
defineProps<IInputProps>();
const modelValue = defineModel<columnValue>();
const emit = defineEmits(["focus", "update:modelValue"]);
</script>
