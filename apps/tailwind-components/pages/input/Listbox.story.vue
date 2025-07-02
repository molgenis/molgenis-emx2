<template>
  <h2 class="text-heading-2xl">Listbox component</h2>
  <form class="mb-6 rounded p-4 text-title">
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
      <div class="p-2 grow">
        <InputLabel for="listbox-placeholder">
          Change the default placeholder text
        </InputLabel>
        <InputString id="listbox-placeholder" v-model="listboxPlaceholder" />
      </div>
    </div>
  </form>
  <div class="mb-6 rounded px-6 py-8 text-title">
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
      @update:model-value="(value: any) => (modelValue = value)"
      @search="(value: string) => searchTerm = value"
    />
    <h4 class="text-heading-md mt-6">Listbox outputs</h4>
    <output class="block w-full mt-2 border py-3 px-2 pl-6">
      <code>Search Term: {{ searchTerm }}</code>
    </output>
    <output class="block w-full mt-6 border py-3 px-2 pl-6">
      <code> User selection: {{ modelValue }} </code>
    </output>
  </div>
  <div class="mb-2 rounded p-6 text-title">
    <h3 class="text-heading-lg mb-2">Input data structure</h3>
    <p>Based on the selection above, the input data is shown below.</p>
    <output
      class="block w-full mt-6 border py-3 px-2 pl-6 h-60 overflow-y-scroll shadow-inner"
    >
      <pre class="indent-[-5em]">
        {{ listboxData }}
      </pre>
    </output>
  </div>
</template>

<script lang="ts" setup>
import { ref, onBeforeMount, watch } from "vue";

interface ListboxDemoData {
  value: string;
  label: string;
}

const modelValue = defineModel<string[] | ListboxDemoData[]>();

const commonBirdNames: string[] = [
  "Kookaburra",
  "Sulphur-crested Cockatoo",
  "Rainbow Lorikeet",
  "Emu",
  "Australian Magpie",
  "Galah",
  "Superb Fairywren",
  "Lyrebird",
  "Budgerigar",
  "Tawny Frogmouth",
];

const commonBirdNamesArray: ListboxDemoData[] = commonBirdNames.map(
  (value: string) => {
    return { value: value.toLowerCase(), label: value };
  }
);

const listboxState = ref<string>("");
const listboxPlaceholder = ref<string>("Select an option");
const listboxDataType = ref<string>("string");
const searchTerm = ref<string>("");

const listboxData = ref<string[] | ListboxDemoData[]>([]);

function updateInputData() {
  if (listboxDataType.value === "string") {
    return commonBirdNames;
  } else {
    return commonBirdNamesArray;
  }
}

onBeforeMount(() => {
  listboxData.value = updateInputData();
});

watch(listboxDataType, () => {
  listboxData.value = updateInputData();
});

watch(searchTerm, () => {
  const data = updateInputData();
  if (searchTerm.value !== "") {
    listboxData.value = data.filter((item) => {
      if (listboxDataType.value === "string") {
        return (item as string).includes(searchTerm.value as string);
      } else {
        return (item as ListboxDemoData).value.includes(
          searchTerm.value as string
        );
      }
    }) as string[] | ListboxDemoData[];
  } else {
    listboxData.value = data;
  }
});
</script>
