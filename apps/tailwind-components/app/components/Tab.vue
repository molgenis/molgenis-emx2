<script setup lang="ts">
defineProps<{
  active?: boolean;
}>();
</script>
<template>
  <div
    class="p-5 mx-[1px] bg-tab hover:bg-tab-hover text-button-outline hover:text-white text-center [text-wrap:nowrap] cursor-pointer"
    :class="{
      'bg-tab-active text-white': active,
    }"
  >
    <slot></slot>
  </div>
</template>
