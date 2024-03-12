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
  <p v-if="text" class="text-body-base mb-5 xl:block hidden">
    {{ truncate ? `${text?.substring(0, cutoff)}` : text }}
    <button
      v-if="truncate && text && text.length > cutoff"
      class="underline italic"
      @click="truncate = false"
    >
      ...
    </button>
    <button
      v-else-if="!truncate && text && text.length > cutoff"
      class="underline italic"
      @click="truncate = true"
    >
      read less
    </button>
  </p>

  <p v-if="text" class="text-body-base mt-5 block xl:hidden">
    {{ truncate ? `${text?.substring(0, cutoff)}...` : text }}
    <button
      v-if="truncate && text && text.length > cutoff"
      class="underline italic"
      @click="truncate = false"
    >
      ...
    </button>
    <button
      v-else-if="!truncate && text && text.length > cutoff"
      class="underline italic"
      @click="truncate = true"
    >
      read less
    </button>
  </p>
</template>
