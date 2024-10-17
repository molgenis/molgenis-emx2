<script setup lang="ts">
const hasError = ref<boolean>(false);
const required = ref<boolean>(false);
const fieldValue = ref<string>("");

const exampleData = [
  { value: "level-1", label: "Level 1 (A)" },
  { value: "level-2", label: "Level 2 (AA)" },
  { value: "level-3", label: "Level 3 (AAA)" },
];

function clearValue() {
  fieldValue.value = "";
}
</script>

<template>
  <div class="flex gap-5">
    <form class="grow">
      <legend class="text-title">Select the level of security required</legend>
      <InputSelect
        id="example-level-input"
        :selectOptions="exampleData"
        v-model="fieldValue"
        :has-error="hasError"
        :required="required"
      />
    </form>

    <form class="m-4 flex-1 flex flex-col">
      <legend class="text-title">Configure the select component</legend>
      <fieldset class="my-2 [&_div]:my-1">
        <legend class="text-title">Adjust props</legend>
        <div>
          <input
            id="input-select-required"
            class="hover:cursor-pointer"
            type="checkbox"
            v-model="required"
          />
          <label class="hover:cursor-pointer" for="input-select-required">
            required
          </label>
        </div>
        <div>
          <input
            id="input-select-has-error"
            class="hover:cursor-pointer"
            type="checkbox"
            v-model="hasError"
          />
          <label class="ml-1 hover:cursor-pointer" for="input-select-has-error">
            hasError
          </label>
        </div>
      </fieldset>
      <h3 class="text-title">Output</h3>
      <output class="my-2 border border-gray-900 py-2 pl-2">
        Selection: {{ fieldValue }}
      </output>
      <Button
        type="outline"
        size="small"
        class="mt-4 mr-auto"
        label="Clear value"
        @click.prevent="clearValue"
      />
    </form>
  </div>
</template>
