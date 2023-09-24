<script setup lang="ts">
defineProps({
  value: {
    type: String,
    required: true,
  },
  cutoff: { type: Number, default: 300 },
});
const truncate = ref(true);
</script>
<template>
  <p class="text-body-base mt-5 block">
    {{ truncate ? `${value?.substring(0, cutoff)}...` : value }}
    <button
      v-if="truncate && value?.length > cutoff"
      class="underline italic"
      @click="truncate = false"
    >
      read more
    </button>
    <button
      v-if="!truncate && value?.length > cutoff"
      class="underline italic"
      @click="truncate = true"
    >
      read less
    </button>
  </p>
</template>
