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
  <a href="#" class="hover:shadow-md transition-shadow border rounded-3px">
    <figure class="p-3.5">
      <img
        :src="url"
        :alt="title"
        class="rounded-3px object-none w-full h-56"
      />
      <figcaption class="mt-3.5 text-body-base text-blue-500">
        {{ title }}
      </figcaption>
    </figure>
  </a>
</template>
