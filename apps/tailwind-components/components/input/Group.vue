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
    <div
      v-if="errorMessage"
      :id="`${id}-input-error`"
      class="bg-invalid text-required p-3 font-bold flex items-center rounded-input"
    >
      <BaseIcon name="info"></BaseIcon>{{ errorMessage }}
    </div>
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
