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
      'mx-3 px-2 py-1 rounded font-bold -mt-1': type === 'form',
    }"
  >
    Draft
  </div>
</template>
