<script setup lang="ts">
const { text, cutoff } = withDefaults(
  defineProps<{
    text?: string;
    cutoff?: number;
  }>(),
  {
    cutoff: 250,
  }
);

let truncate = ref(true);
</script>
<template>
  <p class="text-body-base my-5 xl:block hidden">
    {{ text }}
  </p>

  <p v-if="text" class="text-body-base mt-5 block xl:hidden">
    {{ truncate ? `${text?.substring(0, cutoff)}...` : text }}
  </p>

  <button
    v-if="truncate && text && text.length > cutoff"
    class="my-5 text-blue-500 hover:underline hover:bg-blue-50 mt-5 xl:hidden"
    @click="truncate = false"
  >
    Read more
  </button>
</template>
