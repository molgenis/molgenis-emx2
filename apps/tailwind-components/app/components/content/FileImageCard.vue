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
    :href="url"
    target="_blank"
    class="hover:shadow-md transition-shadow border rounded-base"
  >
    <figure class="p-3.5">
      <img
        :src="url"
        :alt="title"
        class="rounded-base h-56 bg-contain bg-center"
      />
      <figcaption class="mt-3.5 text-body-base text-link">
        {{ title }}
      </figcaption>
    </figure>
  </a>
</template>
