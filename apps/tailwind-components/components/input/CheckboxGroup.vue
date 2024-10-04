<template>
  <div :id="`${id}-checkbox-group`">
    <div
      class="flex justify-start align-center"
      v-for="option in checkboxOptions"
      :key="option.value"
    >
      <InputCheckbox
        :id="`${id}-checkbox-group-${option.value}`"
        class="sr-only"
        :name="id"
        :value="option.value"
        v-model="modelValue"
        :checked="option.value === modelValue"
      />
      <InputCheckboxIcon :checked="option.value === modelValue" />
      <InputLabel
        :for="`${id}-checkbox-group-${option.value}`"
        class="hover:cursor-pointer"
      >
        <template v-if="option.label">
          {{ option.label }}
        </template>
        <template v-else>
          {{ option.value }}
        </template>
      </InputLabel>
    </div>
    <div class="mt-2" v-if="showClearButton">
      <button
        type="reset"
        :id="`${id}-checkbox-group-clear`"
        :form="`${id}-checkbox-group`"
        @click.prevent="resetModelValue"
      >
        Clear
      </button>
    </div>
  </div>
</template>

<script lang="ts" setup>
interface checkboxDataIF {
  value: string;
  label?: string;
  checked?: boolean | undefined;
}
withDefaults(
  defineProps<{
    id: string;
    checkboxOptions: checkboxDataIF[];
    showClearButton?: boolean;
  }>(),
  {
    showClearButton: false,
  }
);

const modelValue = defineModel<string>();

function resetModelValue() {
  modelValue.value = "";
}
</script>
