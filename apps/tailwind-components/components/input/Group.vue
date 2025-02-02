<template>
  <div class="mb-4">
    <div v-if="label">
      <label :for="id" class="text-title font-bold">{{ label }}</label>
      <span class="text-disabled text-body-sm ml-3" v-show="required"
        >Required</span
      >
    </div>
    <div v-if="description" class="text-input-description text-body-sm">
      {{ description }}
    </div>
    <slot></slot>
    <Message v-if="errorMessage" type="error" id="`${id}-input-error`">{{
      errorMessage
    }}</Message>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    id: string;
    label?: string;
    description?: string | null;
    required?: boolean;
    errorMessage?: string | null | undefined;
  }>(),
  {
    required: false,
  }
);
</script>
