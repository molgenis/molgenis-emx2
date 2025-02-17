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
    <template v-if="invalid">
      <span :id="`${id}-state-context`" class="sr-only">error</span>
      <BaseIcon name="exclamation" />
    </template>
    <template v-else-if="valid">
      <span :id="`${id}-state-context`" class="sr-only">success</span>
      <BaseIcon name="check" />
    </template>
    <template v-else>
      <span :id="`${id}-state-context`" class="sr-only">information</span>
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
