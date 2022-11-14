<script setup>
import IconButton from "./IconButton.vue";

defineProps({
  title: {
    type: String,
  },
  name: {
    type: String,
    required: true,
  },
  email: {
    type: String,
  },
  href: {
    type: String,
  },
});
</script>

<template>
  <li
    class="border lg:even:border-l-0 p-11 relative -mb-[1px]"
    data-component-name="name"
  >
    <IconButton
      label="label"
      icon="star"
      class="text-blue-500 absolute right-2 top-2"
    />
    <div class="flex items-start justify-center flex-col h-full">
      <span class="font-bold block" v-if="title">{{ title }}</span>
      {{ name }}
      <a
        class="text-blue-500 block hover:underline"
        v-if="href && email"
        :href="href"
      >
        {{ email }}
      </a>
    </div>
  </li>
</template>
