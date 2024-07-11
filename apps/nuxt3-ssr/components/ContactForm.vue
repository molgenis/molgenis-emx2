<script setup lang="ts">
import type { IFormField, ISelectFormField } from "~/interfaces/types";

defineProps<{
  fields: Record<string, IFormField>;
}>();

defineEmits(["submitForm"]);
</script>

<template>
  <form
    class="relative flex flex-col gap-3"
    @submit.prevent="$emit('submitForm')"
  >
    <div v-for="field in fields" :key="field.name">
      <label
        class="pl-3 text-body-base"
        :for="field.name"
        v-if="field.inputType !== 'string'"
      >
        {{ field.label }}
      </label>

      <InputString
        v-if="field.inputType === 'string'"
        :id="field.name"
        :label="field.label"
        v-model="field.fieldValue"
        :has-error="field.hasError"
      />
      <InputTextArea
        v-else-if="field.inputType === 'textarea'"
        :id="field.name"
        v-model="field.fieldValue"
      />
      <InputSelect
        v-else-if="field.inputType === 'select'"
        :id="field.name"
        v-model="field.fieldValue"
        :placeholder="field.placeholder"
        :options="(field as ISelectFormField).options"
      />
      <div class="pl-3" :class="{ 'text-red-500': field.hasError }">
        {{ field.message }}
      </div>
    </div>
  </form>
</template>
