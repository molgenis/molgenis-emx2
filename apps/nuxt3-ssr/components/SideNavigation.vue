<script setup>
defineProps({
  title: {
    type: String,
  },
  image: {
    type: String,
  },
});
</script>

<template>
  <nav
    class="text-body-base bg-white rounded-t-[3px] rounded-b-[50px] pl-12 py-16 drop-shadow-lg"
  >
    <div class="mb-6 font-display text-heading-4xl">
      <h2 v-if="title">{{ title }}</h2>
      <img v-if="image" :src="image" />
    </div>
    <ul>
      <li class="my-2 hover:font-bold">
        <a href="#">Description</a>
      </li>
      <li class="my-2 hover:font-bold">
        <a href="#">General design</a>
      </li>
      <!-- Active state, classes: border-l-4, border-blue-500, font-bold -->
      <li class="my-2 border-l-4 border-blue-500 pl-4 font-bold">
        <a href="#">Contact & contributors</a>
      </li>
      <li class="my-2 hover:font-bold">
        <a href="#">Variables & topics</a>
      </li>
      <li class="my-2 hover:font-bold">
        <a href="#">Available data & samples</a>
      </li>
      <li class="my-2 hover:font-bold">
        <a href="#">Subpopulations</a>
      </li>
      <li class="my-2 hover:font-bold">
        <a href="#">Collection events</a>
      </li>
      <li class="my-2 hover:font-bold">
        <a href="#">Networks</a>
      </li>
      <li class="my-2 hover:font-bold">
        <a href="#">Partners</a>
      </li>
    </ul>
  </nav>
</template>
