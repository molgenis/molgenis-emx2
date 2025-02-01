<template>
  <InputTestContainer>
    <template v-slot:default="{ placeholder, valid, error, disabled }">
      <div class="flex flex-col gap-2">
        <span v-for="type in Object.keys(demoValue)">
          <Input
            :type="type"
            v-model="demoValue[type]"
            :id="'input-' + type"
            :placeholder="placeholder"
            :valid="valid"
            :disabled="disabled"
            :error="error"
            :errorMessage="errorMessage || null"
            :options="demoOptions"
            :label="'Demo input for type=' + type"
            :required="required"
            refSchemaId="pet store"
            refTableId="Pet"
            refLabel="${name}"
            description="here a demo description to see that that works too"
          />
        </span>
      </div>
    </template>
    <template #settings>
      <Input
        type="string"
        label="errorMessage"
        v-model="errorMessage"
        id="test-container-error-message"
        description="Type here an error message to see how that looks"
      />
      <Input
        type="bool"
        label="required"
        v-model="required"
        id="test-container-required"
        description="set to true to show required tags"
      />
    </template>
  </InputTestContainer>
  <div class="mt-4">value: {{ demoValue }}</div>
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

const errorMessage = ref("");

const required = ref(false);

const demoOptions = ref([
  { label: "option1", value: 1 },
  { label: "option2", value: 2 },
]);
</script>
