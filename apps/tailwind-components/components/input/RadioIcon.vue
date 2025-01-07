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
      class="stroke-gray-600"
      :class="{
        'fill-yellow-500 stroke-none': checked,
      }"
    />
    <circle
      cx="10"
      cy="10"
      r="3"
      stroke-width="1"
      stroke="none"
      :class="{
        'fill-gray-900': checked,
      }"
      v-if="checked"
    />
  </svg>
</template>

<script lang="ts" setup>
defineProps<{
  checked?: boolean;
}>();
</script>
