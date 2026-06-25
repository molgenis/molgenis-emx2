<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import { getSectionOverview } from "../../utils/docsNav";

const route = useRoute();
const slug = computed(() => route.params.slug as string);

const rawModules = import.meta.glob("../../pages/**/*.story.vue", {
  import: "default",
  eager: true,
});

const storyModulePaths = Object.keys(rawModules).map((key) =>
  key.replace("../../pages/", "../pages/")
);

const overview = computed(() =>
  getSectionOverview(slug.value, storyModulePaths)
);
</script>

<template>
  <div class="space-y-8">
    <template v-if="overview">
      <p class="text-body-base text-title">{{ overview.description }}</p>
    </template>
    <p v-else class="text-body-base text-record-subtle">
      Section "{{ slug }}" not found.
    </p>
  </div>
</template>
