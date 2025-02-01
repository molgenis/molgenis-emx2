<script setup lang="ts">
const placeholder = ref("");
const state = ref("default");
</script>

<template>
  <div class="flex">
    <div class="flex-1">
      <slot
        name="default"
        :error="state == 'error'"
        :placeholder="placeholder"
        :valid="state == 'valid'"
        :disabled="state == 'disabled'"
      ></slot>
    </div>
    <div class="flex-1 pl-20">
      <FieldSet label="settings">
        <Input
          type="string"
          id="test-placeholder"
          v-model="placeholder"
          label="Placeholder"
          description="Placeholder of the input, if applicable"
        />
        <Input
          type="radio"
          id="test-state"
          v-model="state"
          label="state"
          description="State of the input"
          class="mt-4"
          :options="[
            { value: 'default', label: 'default' },
            { value: 'error', label: 'Error' },
            { value: 'valid', label: 'Valid' },
            { value: 'disabled', label: 'Disabled' },
          ]"
        />
        <slot name="settings"></slot>
      </FieldSet>
    </div>
  </div>
</template>
