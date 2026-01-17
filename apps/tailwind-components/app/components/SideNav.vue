<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from "vue";

interface Section {
  id: string;
  label: string;
}

const props = withDefaults(
  defineProps<{
    sections: Section[];
    title?: string;
    image?: string;
    scrollOffset?: number;
  }>(),
  {
    scrollOffset: 30,
  }
);

const activeSection = ref<string>("");
let observer: IntersectionObserver | null = null;

function scrollToSection(sectionId: string) {
  const element = document.getElementById(sectionId);
  if (element) {
    const top =
      element.getBoundingClientRect().top + window.scrollY - props.scrollOffset;
    window.scrollTo({ top, behavior: "smooth" });
  }
}

function setupObserver() {
  if (observer) {
    observer.disconnect();
  }

  const options: IntersectionObserverInit = {
    root: null,
    rootMargin: `-${props.scrollOffset}px 0px -50% 0px`,
    threshold: 0,
  };

  observer = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      if (entry.isIntersecting) {
        activeSection.value = entry.target.id;
      }
    }
  }, options);

  for (const section of props.sections) {
    const element = document.getElementById(section.id);
    if (element) {
      observer.observe(element);
    }
  }
}

onMounted(() => {
  setupObserver();
  if (props.sections.length > 0 && props.sections[0]) {
    activeSection.value = props.sections[0].id;
  }
});

watch(
  () => props.sections,
  () => {
    setupObserver();
  },
  { deep: true }
);

onUnmounted(() => {
  if (observer) {
    observer.disconnect();
  }
});
</script>

<template>
  <nav
    class="text-body-base bg-content rounded-t-3px rounded-b-50px px-12 py-16 shadow-primary mb-18"
    aria-label="Section navigation"
  >
    <div v-if="title || image" class="mb-6 font-display text-heading-4xl">
      <img v-if="image" :src="image" :alt="title" class="max-w-full" />
      <span
        v-else
        class="block"
        style="overflow-wrap: break-word; word-wrap: break-word"
      >
        {{ title }}
      </span>
    </div>
    <ul>
      <li v-for="section in sections" :key="section.id">
        <button
          @click="scrollToSection(section.id)"
          class="capitalize w-full block my-2 text-left hover:font-bold hover:cursor-pointer"
          :class="{
            'border-l-4 border-menu-active pl-4 font-bold':
              activeSection === section.id,
          }"
          :aria-current="activeSection === section.id ? 'true' : undefined"
        >
          {{ section.label }}
        </button>
      </li>
    </ul>
  </nav>
</template>
