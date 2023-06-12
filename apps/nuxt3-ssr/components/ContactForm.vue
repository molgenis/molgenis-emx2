<script setup lang="ts">
defineProps<{
  fields: IFormField[];
}>();

defineEmits(["submitForm"]);
</script>

<template>
  <form
    class="relative flex flex-col gap-3"
    @submit.prevent="$emit('submitForm')"
  >
    <div v-for="field in fields" :key="field.name">
      <label class="font-bold text-body-base" :for="field.name">{{
        field.label
      }}</label>

      <FormStringInput
        v-if="field.inputType === 'string'"
        :id="field.name"
        v-model="field.value"
      />
      <FormTextAreaInput
        v-else-if="field.inputType === 'textarea'"
        :id="field.name"
        v-model="field.value"
      />
    </div>
  </form>
</template>
