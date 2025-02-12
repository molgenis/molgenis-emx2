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
      :style="{ 'stroke-width': invalid ? 2 : 1 }"
      fill="none"
      class="stroke-current"
      :class="{
        'fill-input': !checked,
        'fill-input-checked group-hover:fill-input-focused group-focus-within:fill-input-focused':
          checked && !disabled && !invalid && !valid,
        'fill-invalid': checked && invalid,
        'fill-valid': checked && valid,
        'stroke-input peer-focus:stroke-input-focused group-focus-within:stroke-input-focused group-hover:stroke-input-focused':
          !disabled,
        'stroke-valid': valid,
        'stroke-invalid': invalid,
        'stroke-disabled fill-disabled': disabled,
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
  invalid?: boolean;
  valid?: boolean;
  disabled?: boolean;
}>();
</script>
