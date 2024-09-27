<template>
  <div
    :id="`${id}-radio-group`"
    class="flex justify-start align-center"
    v-for="(value, index) in values"
  >
    <InputRadio
      :id="`${id}-radio-group-${value}`"
      class="h-auto"
      :name="id"
      :value="value"
      v-model="modelValue"
      :checked="modelValue === value"
      @change="$emit('update:modelValue', modelValue)"
    />
    <InputLabel
      :for="`${id}-radio-group-${value}`"
      class="hover:cursor-pointer"
    >
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
      :id="`${id}-radio-group-clear`"
      :form="`${id}-radio-group`"
      @click.prevent="onResetSelection"
    >
      Clear
    </button>
  </div>
</template>

<script lang="ts" setup>
withDefaults(
  defineProps<{
    id: string;
    values: string[];
    labels?: string[];
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
