<script setup>
import BaseIcon from "./BaseIcon.vue";
</script>

<template>
  <section class="bg-white py-9 px-12.5 text-gray-900">
    <div class="flex gap-11 flex-wrap items-center">
      <img class="" src="https://via.placeholder.com/162x34" />
      <div class="flex-grow align-middle">
        <a
          href="#"
          target="_blank"
          class="text-blue-500 underline hover:bg-blue-50"
          ><BaseIcon
            name="external-link"
            class="inline mr-2"
          />www.lifelines.nl/next</a
        >
      </div>
      <button
        class="transition-colors rounded-5xl tracking-widest bg-yellow-500 text-gray-900 uppercase text-heading-xl font-display py-2.5 px-7.5 hover:bg-blue-200 mr-5"
      >
        Contact
      </button>
    </div>
  </section>
</template>
