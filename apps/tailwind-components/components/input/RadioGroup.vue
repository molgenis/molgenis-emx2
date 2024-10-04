<template>
  <div :id="`${id}-radio-group`">
    <div
      class="flex justify-start align-center"
      v-for="option in radioOptions"
      :key="option.value"
    >
      <InputRadio
        :id="`${id}-radio-group-${option.value}`"
        class="sr-only"
        :name="id"
        :value="option.value"
        v-model="modelValue"
        :checked="option.value === modelValue"
      />
      <InputLabel
        :for="`${id}-radio-group-${option.value}`"
        class="hover:cursor-pointer flex flex-row gap-1"
      >
        <InputRadioIcon :checked="modelValue === option.value" class="mr-2.5" />
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
        :id="`${id}-radio-group-clear`"
        :form="`${id}-radio-group`"
        @click.prevent="resetModelValue"
      >
        Clear
      </button>
    </div>
  </div>
</template>

<script lang="ts" setup>
interface RadioOptionsDataIF {
  value: string;
  label?: string;
  checked?: boolean | undefined;
}

withDefaults(
  defineProps<{
    id: string;
    radioOptions: RadioOptionsDataIF[];
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
