<script setup lang="ts">
import SideNav from "../components/SideNav.vue";
import ContentBlock from "../components/content/ContentBlock.vue";
import ContentBlocks from "../components/content/ContentBlocks.vue";

const sections = [
  { id: "section-intro", label: "Introduction" },
  { id: "section-features", label: "Features" },
  { id: "section-usage", label: "Usage" },
  { id: "section-examples", label: "Examples" },
  { id: "section-faq", label: "FAQ" },
];
</script>

<template>
  <div class="p-4">
    <h1 class="text-heading-xl mb-4">SideNav Component</h1>
    <p class="text-body-base mb-6">
      Card-style side navigation matching catalogue detail pages. Supports
      optional title or image, and section-based navigation with active state
      tracking.
    </p>

    <div class="xl:flex xl:items-start gap-8">
      <aside class="xl:w-82.5 sticky top-8 shrink-0 hidden xl:block">
        <SideNav title="EXAMPLE" :sections="sections" :scroll-offset="80" />
      </aside>

      <main class="xl:pl-7.5 grow min-w-0">
        <ContentBlocks>
          <ContentBlock
            v-for="section in sections"
            :key="section.id"
            :id="section.id"
            :title="section.label"
          >
            <p class="text-body-base mb-4">
              This is the content for the {{ section.label }} section. Scroll
              down to see the active section highlight change in the side
              navigation.
            </p>
            <p class="text-body-base">
              Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do
              eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut
              enim ad minim veniam, quis nostrud exercitation ullamco laboris
              nisi ut aliquip ex ea commodo consequat.
            </p>
          </ContentBlock>
        </ContentBlocks>
      </main>
    </div>
  </div>
</template>
