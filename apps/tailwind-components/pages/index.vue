<script setup lang="ts">
const modules = import.meta.glob('./*.story.vue', {
  import: 'default',
  eager: true,
})

const stories = Object.entries(modules).map(([key, value]) => {
  const title = key.replace('./', '').replace('.story.vue', '')
  return { title, component: value }
})

</script>

<template>

  <div v-for="story in stories" class="mt-6 pb-12 border-b border-y-gray-300">

    <h3 class="text-heading-3xl my-3">{{ story.title }}</h3>

    <div>
      <component :is="story.component" />
    </div>
  
  </div>
</template>