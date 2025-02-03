<template>
  <InputTestContainer>
    <template v-slot:default="{ placeholder, state }">
      <template v-for="type in Object.keys(demoValue)">
        <FormField
          :type="type"
          v-model="demoValue[type]"
          :id="'input-' + type"
          :placeholder="placeholder"
          :state="state"
          :errorMessage="errorMessage || null"
          :options="demoOptions"
          :label="'Demo input for type=' + type"
          :required="required"
          refSchemaId="pet store"
          refTableId="Pet"
          refLabel="${name}"
          description="here a demo description to see that that works too"
          @blur="blurCount++"
          @focus="focusCount++"
        />
      </template>
    </template>
    <template #settings>
      <FormField
        type="string"
        label="errorMessage"
        v-model="errorMessage"
        id="test-container-error-message"
        description="Type here an error message to see how that looks"
      />
      <FormField
        type="bool"
        label="required"
        v-model="required"
        id="test-container-required"
        description="set to true to show required tags"
      />
      <div class="mt-4">blurCount: {{ blurCount }}</div>
      <div class="mt-4">focusCount: {{ focusCount }}</div>
      <div class="mt-4">value: {{ demoValue }}</div>
    </template>
  </InputTestContainer>
</template>

<script setup lang="ts">
const demoValue: Record<string, any> = ref({
  string: "test string",
  checkbox: [1],
  radio: 1,
  select: 1,
  text: "some demo text",
  ref: null,
  bool: true,
});

const focusCount = ref(0);
const blurCount = ref(0);
const errorMessage = ref("");
const required = ref(false);

const demoOptions = ref([
  { label: "option1", value: 1 },
  { label: "option2", value: 2 },
]);
</script>
