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
  <nav class="text-body-base bg-white rounded-t-3px rounded-b-50px px-12 py-16 shadow-primary">
    <div v-if="title || image" class="mb-6 font-display text-heading-4xl">
      <h2 v-if="title">{{ title }}</h2>
      <img v-if="image" :src="image" />
    </div>
    <ul>
      <li>
        <a href="#" class="w-full block my-2 hover:font-bold hover:cursor-pointer">
          Description
        </a>
      </li>
      <li>
        <a href="#" class="w-full block my-2 hover:font-bold hover:cursor-pointer">
          General design
        </a>
      </li>
      <!-- Active state, classes: border-l-4, border-menu-active, font-bold -->
      <li>
        <a href="#" class="w-full block my-2 border-l-4 menu-active pl-4 font-bold hover:cursor-pointer">
          Contact & contributors
        </a>
      </li>
      <li>
        <a href="#" class="w-full block my-2 hover:font-bold hover:cursor-pointer">
          Variables & topics
        </a>
      </li>
      <li>
        <a href="#" class="w-full block my-2 hover:font-bold hover:cursor-pointer">
          Available data & samples
        </a>
      </li>
      <li>
        <a href="#" class="w-full block my-2 hover:font-bold hover:cursor-pointer">
          Subpopulations
        </a>
      </li>
      <li>
        <a href="#" class="w-full block my-2 hover:font-bold hover:cursor-pointer">
          Collection events
        </a>
      </li>
      <li>
        <a href="#" class="w-full block my-2 hover:font-bold hover:cursor-pointer">
          Networks
        </a>
      </li>
      <li>
        <a href="#" class="w-full block my-2 hover:font-bold hover:cursor-pointer">
          Partners
        </a>
      </li>
    </ul>
  </nav>
</template>
