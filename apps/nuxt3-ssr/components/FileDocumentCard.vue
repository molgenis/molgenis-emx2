<script setup>
defineProps({
  title: {
    type: String,
  },
  url: {
    type: String,
  },
});
</script>

<template>
  <a
    class="border rounded-3px p-5 flex items-center hover:shadow-md transition-shadow"
    :href="url">
    <div class="flex-grow align-middle">
      <span class="text-blue-500 text-body-base">
        {{ title }}
      </span>
    </div>
    <IconButton icon="arrow-down" class="text-blue-500" />
  </a>
</template>
