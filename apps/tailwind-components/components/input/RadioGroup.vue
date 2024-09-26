<template>
  <div :id="`${inputId}-group`" v-for="(value, index) in values" :key="index">
    <InputRadio
      :id="`${inputId}-${value}`"
      :name="name"
      :value="value"
      v-model="modelValue"
      :checked="modelValue === value"
    />
    <InputLabel :for="`${inputId}-${value}`" class="hover:cursor-pointer">
      <template v-if="labels">
        {{ labels[index] }}
      </template>
      <template v-else>
        {{ value }}
      </template>
    </InputLabel>
  </div>
  <div class="mt-2" v-if="showClearButton">
    <button
      type="reset"
      :id="`${inputId}-clear-btn`"
      :form="`${inputId}-group`"
      @click.prevent="modelValue = ''"
    >
      Clear
    </button>
  </div>
</template>

<script lang="ts" setup>
withDefaults(
  defineProps<{
    name: string;
    values: string[];
    labels?: string[];
    showClearButton?: boolean;
  }>(),
  {
    showClearButton: false,
  }
);

const inputId: string = useId();
const modelValue = ref<string>("");
</script>
