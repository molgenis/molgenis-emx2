<template>
  <InputTestContainer show-state v-slot:default="{ valid, invalid, disabled }">
    <InputBoolean
      id="boolean-story-id"
      v-model="modelValue1"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    <div class="my-4">intit empty: {{ modelValue1 }}</div>

    <InputBoolean
      id="boolean-story-id2"
      v-model="modelValue2"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    <div class="my-4">init true: {{ modelValue2 }}</div>

    <InputBoolean
      id="boolean-story-id3"
      v-model="modelValue3"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    <div class="my-4">init false: {{ modelValue3 }}</div>

    <InputBoolean
      id="boolean-story-id4"
      v-model="politeBoolean"
      true-label="Yes please !"
      false-label="No thank you"
      :invalid="invalid"
      :disabled="disabled"
    />
  </InputTestContainer>
  <div class="my-4">Be polite: {{ politeBoolean }}</div>
</template>

<script lang="ts" setup>
const modelValue1 = ref<null>(null);
const modelValue2 = ref<boolean>(true);
const modelValue3 = ref<boolean>(false);
const politeBoolean = ref<boolean>(false);
</script>
