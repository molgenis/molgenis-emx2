<template>
  <td class="px-2 py-1">
    <div :class="cellClasses">
      <InputSelect
        id="permission-cell"
        v-model="internalValue"
        :options="selectOptions"
        :disabled="disabled"
        placeholder="—"
        class="!h-7 !text-sm !px-1 !py-0.5"
      />
    </div>
  </td>
</template>

<script setup lang="ts">
import { computed } from "vue";
import InputSelect from "../../../tailwind-components/app/components/input/Select.vue";

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

const selectOptions = computed(() => {
  const mapped = props.options.map((opt) => {
    if (typeof opt === "string") {
      return props.isGrant && opt === "true" ? "Yes" : opt;
    }
    return String(opt);
  });
  return ["—", ...mapped];
});

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

const internalValue = computed({
  get: () => {
    const val = displayValue.value;
    if (props.isGrant && val === "true") return "Yes";
    return val;
  },
  set: (value: string | number | undefined | null) => {
    const strValue = String(value);
    if (strValue === "—" || strValue === "") {
      emit("update:modelValue", null);
      return;
    }
    if (props.isGrant) {
      emit("update:modelValue", strValue === "Yes" || strValue === "true");
      return;
    }
    emit("update:modelValue", strValue);
  },
});
</script>
