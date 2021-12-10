<template>
  <img :src="url" :alt="alt" class="img-thumbnail" />
</template>

<style></style>

<script>
export default {
  name: "ImageDisplay",
  props: {
    url: {
      type: String,
      required: true,
    },
    alt: {
      type: String,
      required: false,
      default: () => "image",
    },
    id: {
      type: String,
      required: false,
    },
    size: {
      type: Number,
      required: false,
    },
    extension: {
      type: String,
      required: false,
    },
  },
};
</script>
