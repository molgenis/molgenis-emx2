<template>
  <div :id="`${id}-checkbox-group`">
    {{ modelValue }}
    <div
      class="flex justify-start align-center"
      v-for="option in checkboxOptions"
    >
      <InputCheckbox
        :id="`${id}-checkbox-group-${option.value}`"
        class="sr-only"
        :name="id"
        :value="option.value"
        v-model="modelValue"
      />
      <!-- :checked="true" -->
      <!-- :checked="modelValue ? modelValue.indexOf(option.value) > -1 : false" -->
      <InputLabel
        :for="`${id}-checkbox-group-${option.value}`"
        class="hover:cursor-pointer flex flex-row gap-1"
      >
        <!-- :checked="modelValue ? modelValue.indexOf(option.value) > -1 : false" -->
        <InputCheckboxIcon class="mr-2.5" />
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
    showClearButton: true,
  }
);

const modelValue = defineModel<string[] | boolean[]>();

function resetModelValue() {
  modelValue.value = [];
}
</script>
