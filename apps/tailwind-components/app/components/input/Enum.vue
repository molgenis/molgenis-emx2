<template>
  <InputListbox
    v-if="isArray"
    multiple
    enableSearch
    :id="id"
    :options="matchingValues"
    :invalid="invalid"
    :valid="valid"
    :disabled="disabled"
    :describedBy="describedBy"
    v-model="arrayModelValue"
    :showClearButton="!required"
    @search="searchTerm = $event ?? ''"
    @blur="emit('blur')"
    @focus="emit('focus')"
  />
  <InputListbox
    v-else
    enableSearch
    :id="id"
    :options="matchingValues"
    :invalid="invalid"
    :valid="valid"
    :disabled="disabled"
    :describedBy="describedBy"
    v-model="singleModelValue"
    :showClearButton="!required"
    @search="searchTerm = $event ?? ''"
    @blur="emit('blur')"
    @focus="emit('focus')"
  />
</template>

<script lang="ts" setup>
import { computed, ref } from "vue";
import type { IInputProps } from "../../../types/types";
import InputListbox from "./Listbox.vue";

const props = withDefaults(
  defineProps<
    IInputProps & {
      allowedValues: string[];
      isArray?: boolean;
      required?: boolean;
    }
  >(),
  {
    isArray: false,
    required: false,
  }
);

const modelValue = defineModel<string | string[] | null>();
const emit = defineEmits(["blur", "focus"]);
const searchTerm = ref<string>("");

const matchingValues = computed<string[]>(() => {
  const needle = searchTerm.value.toLowerCase();
  return props.allowedValues.filter((value) =>
    value.toLowerCase().includes(needle)
  );
});

const arrayModelValue = computed<string[]>({
  get() {
    return Array.isArray(modelValue.value) ? modelValue.value : [];
  },
  set(value) {
    modelValue.value = value;
  },
});

const singleModelValue = computed<string | null>({
  get() {
    return Array.isArray(modelValue.value) ? null : modelValue.value ?? null;
  },
  set(value) {
    modelValue.value = value;
  },
});
</script>
