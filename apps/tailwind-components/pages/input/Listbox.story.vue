<template>
  <article
    aria-labelledby="listbox-component-title"
    class="[&>p]:mt-3 [&_p]:mb-4"
  >
    <h2 id="listbox-component-title" class="text-heading-2xl">
      Listbox component
    </h2>
    <p>
      The <code>InputListbox</code> component an alternative to the
      <code>InputSelect</code> component. This component functions and behaves
      in the same manner as the select element, but it allows the component to
      be styled with greater detail. The component is flexible in that it allows
      for multiple input types. Options can be generated from an array of values
      (string, number, boolean) or as an array of objects that follow the
      <code>IListboxOption</code> type (<code
        >[{value: "...", label: "..."},...]</code
      >). The return value will always match the input type. When passing data
      into the component, you must also supply the listbox type. There are two
      types to use.
    </p>
    <ul class="pl-6 mb-4 list-disc">
      <li>
        <code>IListboxValue</code>: an array of values (as strings, or
        string[]). For now, this component only supports string arrays.
      </li>
      <li>
        <code>IListboxOption</code>: an array of objects that have a value and a
        label key.(<code>[{value: "...", label: "..."},...]</code>).
      </li>
    </ul>
    <p>
      The following examples demonstrate the use of the listbox components and
      the various configurations that area allowed.
    </p>
  </article>
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
          :radio-options="[
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
          :radioOptions="[
            {
              value: 'disabled',
              label: 'Disabled: disable interactivity with the component',
            },
            {
              value: 'error',
              label: 'Error: simulate an instance where an error occurred',
            },
          ]"
          :showClearButton="true"
        />
      </div>
    </div>
    <div class="bg-white p-2 grow">
      <InputLabel for="listbox-placeholder">
        Change the default placeholder text
      </InputLabel>
      <InputString id="listbox-placeholder" v-model="listboxPlaceholder" />
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
      v-model="listboxSelection"
      :hasError="listboxState === 'error'"
      :disabled="listboxState === 'disabled'"
      :placeholder="listboxPlaceholder"
    />
    <output class="block w-full mt-6 bg-gray-100 py-3 px-2 pl-6">
      <code
        >Output {{ listboxDataType === "true" ? "Value" : "Object" }}:
        {{ listboxSelection }}</code
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
import type { IListboxValue, IListboxOption } from "../../types/listbox";

const letters: string[] = [...Array(26).keys()].map((num) =>
  String.fromCharCode(num + 65)
);

const lettersWithLabels: IListboxOption[] = letters.map((letter: string) => {
  return { value: letter, label: `Group ${letter}` };
});

const listboxState = ref<string>("");
const listboxSelection = ref();
const listboxPlaceholder = ref<string>("Select an option");
const listboxDataType = ref<string>("string");

const listboxData = computed<IListboxOption[] | IListboxValue[]>(() => {
  if (listboxDataType.value === "string") {
    return letters as IListboxValue[];
  } else {
    return lettersWithLabels as IListboxOption[];
  }
});
</script>
