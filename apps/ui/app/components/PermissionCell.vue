<template>
  <td class="px-2 py-1">
    <select
      :value="displayValue"
      :disabled="disabled"
      :class="cellClasses"
      class="w-full px-1 py-0.5 text-sm border rounded bg-white disabled:bg-gray-100 disabled:cursor-not-allowed"
      @change="handleChange"
    >
      <option value="">—</option>
      <option v-for="opt in options" :key="String(opt)" :value="String(opt)">
        {{ optionLabel(String(opt)) }}
      </option>
    </select>
  </td>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  modelValue: string | boolean | null;
  options: (string | { value: boolean; label: string })[];
  inherited?: string | boolean | null;
  disabled?: boolean;
  isGrant?: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: string | boolean | null];
}>();

const displayValue = computed(() => {
  if (props.modelValue !== null && props.modelValue !== undefined) {
    return String(props.modelValue);
  }
  if (props.inherited !== null && props.inherited !== undefined) {
    return String(props.inherited);
  }
  return "";
});

function optionLabel(value: string): string {
  if (props.isGrant && value === "true") return "Yes";
  return value;
}

const isInherited = computed(
  () =>
    (props.modelValue === null || props.modelValue === undefined) &&
    props.inherited !== null &&
    props.inherited !== undefined
);

const cellClasses = computed(() => ({
  "text-gray-400 italic": isInherited.value,
  "font-semibold": !isInherited.value && displayValue.value !== "",
}));

function handleChange(event: Event) {
  const target = event.target as HTMLSelectElement;
  const raw = target.value;
  if (raw === "") {
    emit("update:modelValue", null);
    return;
  }
  if (props.isGrant) {
    emit("update:modelValue", raw === "true");
    return;
  }
  emit("update:modelValue", raw);
}
</script>
