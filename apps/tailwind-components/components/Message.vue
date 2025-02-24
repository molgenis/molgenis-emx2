<template>
  <div
    role="alertdialog"
    :aria-labelledby="`${id}-state-context`"
    class="p-3 font-bold flex items-center rounded-input"
    :class="{
      'bg-invalid text-invalid fill-invalid': invalid,
      'bg-valid text-valid fill-valid': valid,
      'bg-neutral text-neutral': !valid && !invalid,
    }"
  >
    <span :id="`${id}-state-context`" class="sr-only">
      {{ invalid ? "error" : valid ? "success" : "information" }}
    </span>
    <template v-if="invalid">
      <BaseIcon name="exclamation" />
    </template>
    <template v-else-if="valid">
      <BaseIcon name="check" />
    </template>
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    id: string;
    valid?: boolean;
    invalid?: boolean;
  }>(),
  {
    valid: false,
    invalid: false,
  }
);
</script>
