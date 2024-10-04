<template>
  <svg
    width="20"
    height="20"
    view-box="0 0 20 20"
    xmlns="http://www.w3.org/2000/svg"
    class="mt-1.5"
    :class="{
      'fill-yellow-500 stroke-none': checked
    }"
  >
    <path d="M1,11 h 18,11 1,18" />
  </svg>
</template>

<script lang="ts" setup>
withDefaults(defineProps<{ checked?: boolean }>(), {
  checked: false,
});
</script>
