<script setup lang="ts">
import { useNuxtApp } from "#app";
import { ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import PlaygroundNavBar from "../PlaygroundNavBar.vue";
import BaseIcon from "../components/BaseIcon.vue";
import FormLegend from "../components/form/Legend.vue";
import {
  buildDocsSidebar,
  getSectionTitleBySlug,
  getSectionNavForRoute,
} from "../utils/docsNav";

const menuIsOpen = ref<boolean>(true);
const navQuery = ref("");

const modules = import.meta.glob("../**/*.story.vue", {
  import: "default",
  eager: true,
});

const route = useRoute();
const router = useRouter();

const expandedSlug = ref<string | null>(
  getSectionNavForRoute(route.path, Object.keys(modules))?.slug ?? null
);

const legendSections = computed(() =>
  buildDocsSidebar(
    Object.keys(modules),
    route.path,
    expandedSlug.value,
    navQuery.value
  )
);

function handleGoToSection(id: string) {
  if (id.startsWith("/section/")) {
    const slug = id.slice("/section/".length);
    if (expandedSlug.value === slug) {
      expandedSlug.value = null;
    } else {
      expandedSlug.value = slug;
      router.push(id);
    }
  } else {
    router.push(id);
  }
}

const storyName = computed(() => {
  if (route.path.startsWith("/section/")) {
    const slug = route.path.slice("/section/".length);
    return getSectionTitleBySlug(slug) ?? "Section";
  }
  const pathParts = route.path.split("/").filter(Boolean);
  const capitalizedParts = pathParts.map(
    (part) => part.charAt(0).toUpperCase() + part.slice(1)
  );
  return capitalizedParts.join("").replace(".story", "");
});

const { $sourceCodeMap } = useNuxtApp();

const storyRef = ref<{ $el: HTMLElement } | null>(null);

function scrollToTop() {
  if (storyRef.value?.$el) {
    storyRef.value.$el.scrollTo(0, 0);
  }
}
</script>

<template>
  <div
    class="overflow-x-clip min-h-screen bg-base-gradient relative after:bg-app-wrapper after:w-full after:h-[166px] after:top-0 after:absolute after:opacity-20 after:z-20 xl:after:hidden"
  >
    <PlaygroundNavBar @menu-is-open="(value) => (menuIsOpen = value)" />
    <div
      class="absolute top-0 left-0 z-10 w-screen h-screen overflow-hidden opacity-background-gradient"
    >
      <BackgroundGradient class="z-10" />
    </div>
    <div class="z-30 relative mt-[54px] h-[calc(100vh-54px)] overflow-hidden">
      <div id="header-place-holder"></div>
      <div
        class="grid xl:grid-cols-1 h-full"
        :class="{
          'xl:grid-cols-[300px_1fr]': menuIsOpen,
        }"
      >
        <aside
          class="grow hidden xl:flex xl:flex-col overflow-hidden h-full"
          :class="{
            'xl:hidden': !menuIsOpen,
          }"
        >
          <div class="px-4 py-3 shrink-0">
            <label for="docs-nav-search" class="sr-only"
              >Filter navigation</label
            >
            <InputSearch
              id="docs-nav-search"
              v-model="navQuery"
              placeholder="Search..."
              size="small"
            />
          </div>
          <div class="overflow-y-auto grow">
            <FormLegend
              :sections="legendSections"
              @go-to-section="handleGoToSection"
            />
          </div>
        </aside>
        <Story
          ref="storyRef"
          :title="storyName"
          class="min-h-0 h-full overflow-hidden"
        >
          <slot></slot>
        </Story>
      </div>
    </div>
    <div
      class="hidden z-40 fixed bottom-0 left-0 w-[100%] md:flex justify-end px-6 pb-4"
    >
      <Button size="small" type="outline" id="scrollToTop" @click="scrollToTop">
        <BaseIcon name="ArrowUp" :width="24" />
        <span class="sr-only">scroll to top</span>
      </Button>
    </div>
  </div>
</template>
