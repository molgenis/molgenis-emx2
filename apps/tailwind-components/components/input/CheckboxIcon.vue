<template>
  <svg
    width="20"
    height="20"
    view-box="0 0 20 20"
    xmlns="http://www.w3.org/2000/svg"
    class="w-[20px] mr-2.5"
    :data-checked="checked"
    :data-indeterminate="indeterminate"
  >
    <rect
      width="20"
      height="20"
      :style="{ 'stroke-width': invalid ? 4 : 2 }"
      :class="{
        'fill-input': !checked && !indeterminate,
        'fill-input-checked group-hover:fill-input-focused group-focus-within:fill-input-focused':
          (checked || indeterminate) && !disabled,
        'fill-invalid': checked && invalid,
        'fill-valid': checked && valid,
        'stroke-input peer-focus:stroke-input-focused group-focus-within:stroke-input-focused group-hover:stroke-input-focused':
          !disabled,
        'stroke-valid': valid,
        'stroke-invalid': invalid,
        'stroke-disabled fill-disabled': disabled,
      }"
    />
    <path
      v-if="checked"
      d="M5,10 L 9,14 L 16,6"
      class="stroke-input-checked"
      :class="{
        'stroke-disabled': disabled,
      }"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      fill="none"
    />
    <path
      v-if="indeterminate"
      d="M5,10 L 15,10"
      class="stroke-input-checked"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      fill="none"
    />
  </svg>
</template>

<script lang="ts" setup>
defineProps<{
  checked?: boolean;
  indeterminate?: boolean;
  invalid?: boolean;
  valid?: boolean;
  disabled?: boolean;
  focused?: boolean;
}>();
</script>
