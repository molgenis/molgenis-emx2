<script setup lang="ts">
const route = useRoute();
const props = defineProps<{
  title?: string;
  image?: string;
  items: { label: string, id: string }[]
}>();

let currentSection = ref()

onMounted(() => {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.intersectionRatio > 0) {
        console.log(entry.target.parentElement?.id)
        console.log(entry.intersectionRatio)
        currentSection.value = entry.target.parentElement?.id
      }
    })
  }, { rootMargin: '0px 0px -90% 0px'})
  document.querySelectorAll('section h2').forEach((section) => {
    observer.observe(section)
  })
})

function setSideMenuStyle(hash: string) {
  if ('#' + currentSection === hash || hash == route.hash) {
    return "w-full block my-2 border-l-4 menu-active pl-4 font-bold hover:cursor-pointer"
  } else {
    "w-full block my-2 hover:font-bold hover:cursor-pointer"
  }
}

</script>

<template>
  <nav class="text-body-base bg-white rounded-t-3px rounded-b-50px px-12 py-16 shadow-primary">
    <div v-if="title || image" class="mb-6 font-display text-heading-4xl">
      <img v-if="image" :src="image" />
      <h2 v-else if="title">{{ title }}</h2>
    </div>
    <ul>
      <li v-for="item in items">
        <a :href="('#' + item.id)" :class="setSideMenuStyle('#' + item.id)">
          {{ item.label }}
        </a>
      </li>
    </ul>
  </nav>
</template>
