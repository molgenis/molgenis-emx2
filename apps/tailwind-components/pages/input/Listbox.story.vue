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
        <code>IListboxValueArray</code>: an array of values (as strings, or
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
  <form class="mb-4 bg-white rounded p-4">
    <legend class="mb-2">Configure the listbox component</legend>
    <div class="mb-4">
      <InputLabel for="listbox-component-state">
        Select the component state
      </InputLabel>
      <InputRadioGroup
        id="listbox-component-state"
        v-model="listboxState"
        :radioOptions="[
          {
            value: 'disabled',
            label: 'Component is disabled',
          },
          {
            value: 'error',
            label: 'Component has error',
          },
        ]"
        :showClearButton="true"
      />
    </div>
    <div class="mb-4">
      <InputLabel for="listbox-placeholder">
        Change the default placeholder text
      </InputLabel>
      <InputString id="listbox-placeholder" v-model="listboxPlaceholder" />
    </div>
  </form>
  <div class="mb-2 bg-white rounded p-4">
    <InputLabel id="listbox-input-label" for="listbox-input" class="block mb-3">
      <span>Select a group assignment</span>
    </InputLabel>
    <InputListbox
      id="listbox-input"
      labelId="listbox-input-label"
      :options="optionsAsValueArrays"
      v-model="listboxSelection"
      :hasError="listboxState === 'error'"
      :disabled="listboxState === 'disabled'"
      :placeholder="listboxPlaceholder"
    />
    <output class="block w-full mt-6 bg-white py-2 px-2">
      <code>Output: {{ listboxSelection }}</code>
    </output>
  </div>
</template>

<script lang="ts" setup>
import { ref } from "vue";
import type { IListboxValueArray, IListboxOption } from "../../types/listbox";

// as ListboxOption
const letters = [...Array(26).keys()].map((num) =>
  String.fromCharCode(num + 65)
);

const optionsWithLabelsData = ref<IListboxOption[]>(
  letters.map((letter: string) => {
    return { value: letter, label: "Group" + letter };
  })
);

// as Listbox Array
const optionsAsValueArrays = ref<IListboxValueArray[]>(letters);

const listboxState = ref<string>("");
const listboxSelection = ref();
const listboxPlaceholder = ref<string>("Select an option");
</script>
