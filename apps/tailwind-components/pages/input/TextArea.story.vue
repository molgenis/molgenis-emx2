<script setup lang="ts">
const id = "story-input-text-area-1";
const hasError = ref(false);
const required = ref(false);
const fieldValue = ref("");
</script>

<template>
  <div class="flex">
    <div class="m-4 flex-1">
      <div>
        <InputTextArea
          :id="id"
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
          @click="fieldValue = ''"
          class="bg-blue-500 hover:bg-blue-700 text-white py-2 px-4 rounded"
        >
          clear value
        </button>
      </div>

      <fieldset class="mt-4 border border-gray-900 mb-2">
        <legend class="m-2 px-2">Props</legend>
        <div class="mb-2">
          <label
            class="ml-1 hover:cursor-pointer"
            for="input-text-area-required"
          >
            required
          </label>
          <input
            id="input-text-area-required"
            class="ml-2 hover:cursor-pointer"
            type="checkbox"
            v-model="required"
          />
        </div>
        <div class="mb-2">
          <label
            class="ml-1 hover:cursor-pointer"
            for="input-text-area-has-error"
          >
            hasError
          </label>
          <input
            id="input-text-area-has-error"
            class="ml-2 hover:cursor-pointer"
            type="checkbox"
            v-model="hasError"
          />
        </div>
      </fieldset>
    </div>
  </div>
</template>
