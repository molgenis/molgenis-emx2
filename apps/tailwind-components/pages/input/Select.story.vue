<script setup lang="ts">
const id = "story-input-1";
const hasError = ref(false);
const required = ref(false);
const fieldValue = ref("");

const options = ["option 1", "option 2", "option 3", "option 4"];

function clearValue() {
  fieldValue.value = "";
}
</script>

<template>
  <div class="flex">
    <div class="m-4 flex-3">
      <div>
        <InputSelect
          :id="id"
          :options="options"
          v-model="fieldValue"
          :has-error="hasError"
          :required="required"
        />
      </div>
    </div>

    <div class="m-4 flex-1">
      <p>fieldValue value: {{ fieldValue }}</p>

      <div class="mt-4">
        <button
          @click="clearValue"
          class="bg-blue-500 hover:bg-blue-700 text-white py-2 px-4 rounded"
        >
          clear value
        </button>
      </div>

      <fieldset class="mt-4 border border-gray-900 mb-2">
        <legend class="m-2 px-2">Props</legend>
        <div class="mb-2">
          <label class="ml-1 hover:cursor-pointer" for="input-select-required">
            required
          </label>
          <input
            id="input-select-required"
            class="ml-2 hover:cursor-pointer"
            type="checkbox"
            v-model="required"
          />
        </div>
        <div class="mb-2">
          <label class="ml-1 hover:cursor-pointer" for="input-select-has-error">
            hasError
          </label>
          <input
            id="input-select-has-error"
            class="ml-2 hover:cursor-pointer"
            type="checkbox"
            v-model="hasError"
          />
        </div>
      </fieldset>
    </div>
  </div>
</template>
