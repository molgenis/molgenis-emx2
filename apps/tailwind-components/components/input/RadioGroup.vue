<template>
  <div
    :id="`${id}-radio-group`"
    class="flex justify-start align-center"
    v-for="option in radioOptions"
    :key="option.value"
  >
    <InputRadio
      :id="`${id}-radio-group-${option.value}`"
      class="h-auto"
      :name="id"
      :value="option.value"
      v-model="modelValue"
      :checked="
        option.value || option.checked
          ? isChecked(option.value, option.checked)
          : null
      "
      @change="emitModelValue"
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
      @click.prevent="resetModelValue"
    >
      Clear
    </button>
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

const modelValue = ref<string>("");
const emit = defineEmits(["update:modelValue"]);

function isChecked(value: string, checked: boolean | undefined) {
  if (checked) {
    console.log(value, "is checked");
    modelValue.value = value;
    return "checked";
  } else {
    return null;
  }
}

function emitModelValue() {
  emit("update:modelValue", modelValue.value);
}

function resetModelValue() {
  modelValue.value = "";
  emitModelValue();
}
</script>
