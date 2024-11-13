<template>
  <h2>Listbox component</h2>
  <p class="mb-2">
    The <code>InputListbox</code> component is an reimplementation of the
    <code>InputSelect</code> component. The component functions and behaves in
    the same manner as the select element, but it allows the component to be
    styled.
  </p>
  <p class="mb-2">
    The following example demonstrates the use of the listbox components with
    various configurations.
  </p>
  <form class="mb-2 bg-white rounded p-2">
    <legend>Configure the listbox component</legend>
    <div class="mb-2">
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
    <div class="mb-2">
      <InputLabel for="listbox-placeholder">
        Change the default placeholder text
      </InputLabel>
      <InputString id="listbox-placeholder" v-model="listboxPlaceholder" />
    </div>
  </form>
  <div class="mb-2 bg-white rounded p-2">
    <InputLabel id="listbox-input-label" for="listbox-input" class="block mb-3">
      <span>Select a group assignment</span>
    </InputLabel>
    <InputListbox
      id="listbox-input"
      labelId="listbox-input-label"
      :options="data"
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
const data = [...Array(26).keys()].map((num: number) => {
  return {
    value: num,
    label: "Group " + String.fromCharCode(num + 65),
  };
});

const listboxState = ref<string>("");
const listboxSelection = ref();
const listboxPlaceholder = ref<string>("Select an option");
</script>
