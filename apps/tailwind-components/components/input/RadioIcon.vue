<template>
  <svg
    height="20"
    width="20"
    view-box="0 0 20 20"
    xmlns="http://www.w3.org/2000/svg"
    class="mt-1.5"
  >
    <circle
      cx="10"
      cy="10"
      r="9"
      stroke-width="1"
      fill="none"
      :class="{
        'fill-input hover:fill-input-checked hover:stroke-none focus:fill-input-checked focus:stroke-none':
          !checked,
        'fill-input-checked stroke-none': checked && !error && !valid,
        'fill-invalid': checked && error,
        'fill-valid': checked && valid,
        'stroke-current ': !error && !valid,
        'stroke-invalid': error,
        'stroke-valid': valid,
      }"
    />
    <circle
      cx="10"
      cy="10"
      r="3"
      stroke-width="1"
      stroke="none"
      class="fill-transparent"
      :class="{
        '!fill-check': checked,
      }"
    />
  </svg>
</template>

<script lang="ts" setup>
defineProps<{
  checked?: boolean;
  error?: boolean;
  valid?: boolean;
}>();
</script>
