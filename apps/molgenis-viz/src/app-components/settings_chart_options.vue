<template>
  <InputLabel :id="id" :label="label" :description="description" />
  <select
    :id="id"
    :data-for="inputFor"
    @change="onChange"
    class="select_options_input"
  >
    <option v-if="includeDefault" value="" disabled selected>
      {{ defaultLabel }}
    </option>
    <option v-for="option in options" :value="option.name">
      {{ option.label ? option.label : option.name }}
    </option>
  </select>
</template>

<script setup lang="ts">
import InputLabel from "../components/forms/InputLabel.vue";

interface Options {
  name: String;
  label?: String;
}

interface Props {
  id: string;
  label: string;
  inputFor?: string;
  description?: string;
  includeDefault?: boolean;
  defaultLabel?: string;
  options?: Options[];
  optionName?: string;
  optionLabel?: string;
}

withDefaults(defineProps<Props>(), {
  includeDefault: true,
  defaultLabel: "Select an option",
  optionName: "name",
});

const emit = defineEmits(["input"]);

function onChange(event: Event) {
  const value = (event.target as HTMLInputElement).value;
  const inputFor = (event.target as HTMLInputElement).getAttribute("data-for");
  emit("input", { for: inputFor, value: value });
}
</script>

<style lang="scss">
.select_options_input {
  @include textInput;
  margin: 0;
  font-size: 1rem;

  &:focus {
    @include inputFocus;
  }
}
</style>
