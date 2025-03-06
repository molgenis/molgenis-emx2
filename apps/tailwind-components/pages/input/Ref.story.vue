<script setup lang="ts">
const schemaId = ref<string>("pet store");
const tableId = ref<string>("Pet");
const labelTemplate = ref<string>("${name}");
const value = ref([{ name: "spike" }]);
const value2 = ref([{ name: "spike" }]);

const refExampleError = ref<boolean>(false);

function onBlur() {
  if (!value.value.length) {
    refExampleError.value = true;
  } else {
    refExampleError.value = false;
  }
}
</script>

<template>
  <InputTestContainer show-state v-slot="{ invalid, valid, disabled }">
    <div>
      <label for="story-ref-array">
        <span class="text-title font-bold">Select pets by name</span>
        <span class="text-disabled text-body-sm ml-3"> Required </span>
      </label>
      <InputRef
        id="story-ref-array"
        v-model="value"
        :refSchemaId="schemaId"
        :refTableId="tableId"
        :limit="5"
        :refLabel="labelTemplate"
        :valid="valid"
        :invalid="refExampleError || invalid"
        :disabled="disabled"
        :required="true"
        @update:modelValue="onBlur"
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
          <span>An error was thrown</span>
        </Message>
      </div>
      <div class="pt-5">value selected: {{ value }}</div>
    </div>
    <div class="pt-5">
      <label for="story-ref-array">
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
    </div>
    <div class="pt-5">value selected: {{ value2 }}</div>
  </InputTestContainer>
</template>
