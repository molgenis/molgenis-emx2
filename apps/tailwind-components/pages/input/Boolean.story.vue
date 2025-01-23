<template>
  <InputBoolean id="boolean-story-id" v-model="modelValue1" />

  <div class="my-4">intit empty: {{ modelValue1 }}</div>

  <InputBoolean id="boolean-story-id2" v-model="modelValue2" />

  <div class="my-4">init true: {{ modelValue2 }}</div>

  <InputBoolean id="boolean-story-id3" v-model="modelValue3" />

  <div class="my-4">init false: {{ modelValue3 }}</div>
</template>

<script lang="ts" setup>
const modelValue1 = ref<boolean | undefined>(null);
const modelValue2 = ref<boolean | undefined>(true);
const modelValue3 = ref<boolean | undefined>(false);
</script>
