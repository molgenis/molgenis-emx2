<script setup lang="ts">
import { ref } from "vue";

const schemaId = ref<string>("pet store");
const tableId = ref<string>("Pet");
const labelTemplate = ref<string>("${name}");
const value1 = ref([{ name: "spike" }]);
const value2 = ref([{ name: "spike" }]);

const refExampleError = ref<boolean>(false);

function onBlur() {
  if (!value1.value.length) {
    refExampleError.value = true;
  } else {
    refExampleError.value = false;
  }
}
</script>

<template>
  <InputTestContainer show-state v-slot="{ invalid, valid, disabled }">
    <div class="mb-4">
      <label for="story-ref-array">
        <span class="text-title font-bold">Select pets by name</span>
        <span class="text-disabled text-body-sm ml-3"> Required </span>
      </label>
      <InputRef
        id="story-ref-array"
        v-model="value1"
        :refSchemaId="schemaId"
        :refTableId="tableId"
        :limit="5"
        :refLabel="labelTemplate"
        :valid="valid"
        :invalid="refExampleError || invalid"
        :disabled="disabled"
        :required="true"
        @update:modelValue="refExampleError ? onBlur() : null"
        @blur="onBlur"
      />
      <div id="story-ref-array-error-container">
        <Message
          id="story-ref-array-error"
          :invalid="refExampleError"
          v-if="refExampleError"
        >
          <span>Field is required</span>
        </Message>
        <Message id="story-ref-array-error" :invalid="invalid" v-if="invalid">
          <span>A generic error message</span>
        </Message>
      </div>
      <h3 class="mt-4 text-title">Selected value</h3>
      <StoryComponentOutput>
        <code>{{ value1 }}</code>
      </StoryComponentOutput>
    </div>
    <div class="pt-5">
      <label for="story-ref" class="block mb-2">
        <span class="text-title font-bold">Select pets by name</span>
      </label>
      <InputRef
        id="story-ref"
        v-model="value2"
        :refSchemaId="schemaId"
        :refTableId="tableId"
        :limit="5"
        :refLabel="labelTemplate"
        :isArray="false"
        :valid="valid"
        :invalid="invalid"
        :disabled="disabled"
      />
      <h3 class="mt-4 text-title">Selected value</h3>
      <StoryComponentOutput>
        <code>{{ value2 }}</code>
      </StoryComponentOutput>
    </div>
  </InputTestContainer>
</template>
