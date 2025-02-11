<template>
  <div
    role="alertdialog"
    :aria-labelledby="`${id}-state-context`"
    class="p-3 font-bold flex items-center rounded-input bg-default"
    :class="{
      'bg-invalid text-invalid fill-invalid': invalid,
      'bg-valid text-valid fill-valid': valid,
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
      <BaseIcon name="info" />
    </template>
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    id: string;
    invalid?: boolean;
    valid?: boolean;
  }>(),
  {
    invalid: false,
    valid: false,
  }
);
</script>
