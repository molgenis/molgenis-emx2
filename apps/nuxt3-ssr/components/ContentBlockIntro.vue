<script setup>
import BaseIcon from "./BaseIcon.vue";
defineProps({
  image: {
    type: String,
    required: true,
  },
  link: {
    type: String,
    required: true,
  },
  linkTarget: {
    type: String,
    default: "_blank",
  },
  contact: {
    type: String,
    required: true,
  },
  contactTarget: {
    type: String,
    default: "_blank",
  },
});
</script>

<template>
  <section class="bg-white py-9 px-12.5 text-gray-900">
    <div class="flex gap-11 flex-wrap items-center">
      <img class="max-h-11" :src="image" />
      <div class="flex-grow align-middle">
        <a
          :href="link"
          :target="linkTarget"
          class="text-blue-500 underline hover:bg-blue-50"
        >
          <BaseIcon name="external-link" class="inline mr-2" />{{ link }}
        </a>
      </div>
      <a
        :href="contact"
        :target="contactTarget"
        class="
          transition-colors
          rounded-full
          tracking-widest
          bg-yellow-500
          text-gray-900
          uppercase
          text-heading-xl
          font-display
          py-2.5
          px-7.5
          hover:bg-blue-200
          mr-5
        "
      >
        Contact
      </a>
    </div>
  </section>
</template>
