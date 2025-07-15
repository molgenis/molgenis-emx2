<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    small?: boolean;
    compact?: boolean;
  }>(),
  {
    small: false,
    compact: true,
  }
);

const useGridClasses = `grid md:grid-cols-3 ${
  props.compact ? "md:gap-2.5" : "md:gap-4"
}`;
const smallClasses = "";
</script>

<template>
  <dl class="grid gap-2.5 text-body-base">
    <div :class="small ? smallClasses : useGridClasses">
      <slot></slot>
    </div>
  </dl>
</template>
