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
      <label class="pl-3 text-body-base" :for="field.name">{{
        field.label
      }}</label>

      <FormStringInput
        v-if="field.inputType === 'string'"
        :id="field.name"
        v-model="field.fieldValue"
        :has-error="field.hasError"
      />
      <FormTextAreaInput
        v-else-if="field.inputType === 'textarea'"
        :id="field.name"
        v-model="field.fieldValue"
      />
      <div class="pl-3" :class="{ 'text-red-500': field.hasError }">
        {{ field.message }}
      </div>
    </div>
  </form>
</template>
