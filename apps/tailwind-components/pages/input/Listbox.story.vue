<template>
  <h2 class="text-heading-2xl">Listbox component</h2>
  <form class="mb-6 bg-white rounded p-4">
    <legend class="mb-2 text-heading-lg">
      Configure the listbox component
    </legend>
    <div class="flex flex-row flex-wrap justify-center items-start gap-4">
      <div class="p-2 grow">
        <InputLabel for="listbox-data-type" class="pl-0">
          <span>Select the input data type</span>
        </InputLabel>
        <InputRadioGroup
          id="listbox-data-type"
          v-model="listboxDataType"
          :options="[
            { value: 'string', label: 'String array' },
            { value: 'array', label: 'Array of options' },
          ]"
        />
      </div>
      <div class="p-2 grow">
        <InputLabel for="listbox-component-state" class="pl-0">
          Change the component state
        </InputLabel>
        <InputRadioGroup
          id="listbox-component-state"
          v-model="listboxState"
          :options="[
            {
              value: 'disabled',
              label: 'Disabled',
            },
            {
              value: 'error',
              label: 'Error',
            },
            {
              value: 'valid',
              label: 'Valid',
            },
          ]"
          :showClearButton="true"
        />
      </div>
      <div class="bg-white p-2 grow">
        <InputLabel for="listbox-placeholder">
          Change the default placeholder text
        </InputLabel>
        <InputString id="listbox-placeholder" v-model="listboxPlaceholder" />
      </div>
    </div>
  </form>
  <div class="mb-6 bg-white rounded px-6 py-8">
    <h3 class="text-heading-lg mb-2">Listbox example</h3>
    <InputLabel
      id="listbox-input-label"
      for="listbox-input"
      class="block mb-3 pl-0"
      :disabled="listboxState === 'disabled'"
      :has-error="listboxState === 'error'"
    >
      <span>Select a group assignment</span>
    </InputLabel>
    <InputListbox
      id="listbox-input"
      labelId="listbox-input-label"
      :options="listboxData"
      :invalid="listboxState === 'error'"
      :valid="listboxState === 'valid'"
      :disabled="listboxState === 'disabled'"
      :placeholder="listboxPlaceholder"
      @update:model-value="(value) => (modelValue = value)"
    />
    <output class="block w-full mt-6 bg-gray-100 py-3 px-2 pl-6">
      <code
        >Output {{ listboxDataType === "true" ? "Value" : "Object" }}:
        {{ modelValue }}</code
      >
    </output>
  </div>
  <div class="mb-2 bg-white rounded p-6">
    <h3 class="text-heading-lg mb-2">Input data structure</h3>
    <p>Based on the selection above, the input data is shown below.</p>
    <output
      class="block w-full mt-6 bg-gray-100 py-3 px-2 pl-6 h-60 overflow-y-scroll shadow-inner"
    >
      <pre class="indent-[-5em]">
        {{ listboxData }}
      </pre>
    </output>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from "vue";
import type {
  IInputValue,
  IInputValueLabel,
} from "../../../metadata-utils/src/types";

const modelValue = defineModel<IInputValue | IInputValueLabel>();
const letters: string[] = [...Array(26).keys()].map((num) =>
  String.fromCharCode(num + 65)
);

const lettersWithLabels: IInputValueLabel[] = letters.map((letter: string) => {
  return { value: letter, label: `Group ${letter}` };
});

const listboxState = ref<string>("");
const listboxPlaceholder = ref<string>("Select an option");
const listboxDataType = ref<string>("string");

const listboxData = computed<IInputValue[] | IInputValueLabel[]>(() => {
  if (listboxDataType.value === "string") {
    return letters as IInputValue[];
  } else {
    return lettersWithLabels as IInputValueLabel[];
  }
});
</script>
