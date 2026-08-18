<script setup lang="ts">
withDefaults(defineProps<{ disabled?: boolean }>(), { disabled: false });
</script>

<template>
  <tr :class="{ 'sm:hover:bg-hover sm:hover:cursor-pointer': !disabled }">
    <slot></slot>
  </tr>
</template>
