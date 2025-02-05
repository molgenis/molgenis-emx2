<template>
  <div
    role="alertdialog"
    :aria-labelledby="invalid ? id + '-error-state' : ''"
    class="p-3 font-bold flex items-center rounded-input bg-default'"
    :class="{
      'bg-invalid text-required': invalid,
      'bg-valid text-valid': valid,
    }"
  >
    <span v-if="invalid" :id="`${id}-error-state`" class="sr-only">error</span>
    <BaseIcon name="info"></BaseIcon>
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  id: string;
  invalid?: boolean;
  valid?: boolean;
}>();
</script>
