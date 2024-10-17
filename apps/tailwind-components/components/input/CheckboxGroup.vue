<template>
  <div :id="`${id}-checkbox-group`">
    <div class="flex flex-row" v-for="option in checkboxOptions">
      <input
        type="checkbox"
        :id="`${id}-${option.value}`"
        :name="id"
        :value="option.value"
        v-model="modelValue"
        :checked="modelValue!.includes(option.value)"
        class="sr-only"
      />
      <InputLabel
        :for="`${id}-${option.value}`"
        class="hover:cursor-pointer flex justify-start items-center"
      >
        <InputCheckboxIcon :checked="modelValue!.includes(option.value)" />
        <span class="block" v-if="option.label">
          {{ option.label }}
        </span>
        <span class="block" v-else>
          {{ option.value }}
        </span>
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

const modelValue = defineModel<string[]>();

function resetModelValue() {
  modelValue.value = [];
}
</script>
