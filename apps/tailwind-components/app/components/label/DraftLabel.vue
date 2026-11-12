<script setup lang="ts">
withDefaults(
  defineProps<{
    type?: "inline" | "form";
  }>(),
  {
    type: "form",
  }
);
</script>

<template>
  <div
    class="bg-label-draft text-label-draft inline-block"
    :class="{
      'px-1 h-6 text-body-sm rounded align-middle': type === 'inline',
      ' h-8 mx-3 px-2 py-1 rounded font-bold': type === 'form',
    }"
  >
    Draft
  </div>
</template>
