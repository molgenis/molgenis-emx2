<script setup lang="ts">
const modules = import.meta.glob("./**/*.story.vue", {
  import: "default",
  eager: true,
});

const stories = Object.entries(modules).map(([key, value]) => {
  const title = key.replace("./", "").replace(".story.vue", "");
  return { title, component: value };
});
</script>

<template>
  <div
    v-for="story in stories"
    class="mt-1 px-4 pt-2 pb-12 first:mt-8 last:mb-8 border-b-2"
  >
    <h3 class="text-heading-4xl my-3">{{ story.title }}</h3>

    <div>
      <component :is="story.component" />
    </div>
  </div>
</template>
