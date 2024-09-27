<template>
  <div
    :id="`${id}-radio-group`"
    class="flex justify-start align-center"
    v-for="option in radioOptions"
  >
    <InputRadio
      :id="`${id}-radio-group-${option.value}`"
      class="h-auto"
      :name="id"
      :value="option.value"
      v-model="modelValue"
      :checked="modelValue === option.value"
      @change="$emit('update:modelValue', modelValue)"
    />
    <InputLabel
      :for="`${id}-radio-group-${option.value}`"
      class="hover:cursor-pointer"
    >
      <template v-if="Object.hasOwn(option, 'label')">
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
      @click.prevent="onResetSelection"
    >
      Clear
    </button>
  </div>
</template>

<script lang="ts" setup>

interface RadioOptionsDataIF {
  value: string;
  label?: string;
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

const modelValue = ref<string>("");
const emit = defineEmits(["update:modelValue"]);

function onResetSelection() {
  modelValue.value = "";
  emit("update:modelValue", modelValue.value);
}
</script>
