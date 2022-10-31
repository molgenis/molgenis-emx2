<script setup>
import ContentBlock from "./ContentBlock.vue";
import List from "./List.vue";

defineProps({
  title: {
    type: String,
  },
  description: {
    type: String,
  },
});
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <button
      class="tracking-widest bg-blue-500 text-white uppercase text-heading-xl font-display py-2.5 px-7.5 rounded-full hover:bg-blue-300 border mr-5"
    >
      All 299 variables
    </button>
    <button
      class="tracking-widest text-blue-500 bg-white uppercase text-heading-xl font-display py-2.5 px-7.5 rounded-full hover:text-blue-300 hover:border-blue-300 border"
    >
      Harmonized variables
    </button>

    <List title="Variables per topic" class="font-bold text-body-base mt-16">
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline">Administrative information</a>
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </li>
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline">Administrative information</a>
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </li>
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline"
          >Birth pregnany and reproductive health history</a
        >
        <span class="text-gray-400 inline-block ml-1">- 46</span>
      </li>
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline"
          >Birth pregnany and reproductive health history</a
        >
        <span class="text-gray-400 inline-block ml-1">- 46</span>
      </li>
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline">Child</a>
        <span class="text-gray-400 inline-block ml-1">- 46</span>
      </li>
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline">Child</a>
        <span class="text-gray-400 inline-block ml-1">- 46</span>
      </li>
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline"
          >Cognition, personality and psychological measure</a
        >
        <span class="text-gray-400 inline-block ml-1">- 46</span>
      </li>
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline"
          >Cognition, personality and psychological measure</a
        >
        <span class="text-gray-400 inline-block ml-1">- 46</span>
      </li>
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline">Death</a>
        <span class="text-gray-400 inline-block ml-1">- 46</span>
      </li>
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline">Death</a>
        <span class="text-gray-400 inline-block ml-1">- 46</span>
      </li>
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline">Diseases</a>
        <span class="text-gray-400 inline-block ml-1">- 46</span>
      </li>
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline">Diseases</a>
        <span class="text-gray-400 inline-block ml-1">- 46</span>
      </li>
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline">Lifestyle & behaviours</a>
        <span class="text-gray-400 inline-block ml-1">- 46</span>
      </li>
      <li class="pl-2.5 font-normal text-blue-500">
        <span class="absolute -ml-2.5">›</span>
        <a href="#" class="hover:underline">Lifestyle & behaviours</a>
        <span class="text-gray-400 inline-block ml-1">- 46</span>
      </li>
    </List>
  </ContentBlock>
</template>
