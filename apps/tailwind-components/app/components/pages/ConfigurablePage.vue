<script setup lang="ts">
import type { IConfigurablePages } from "../../../types/cms";

import PageComponent from "./PageComponent.vue";
import PageBanner from "../pages/Banner.vue";
import PageSection from "../pages/Section.vue";
import TextParagraph from "./Paragraph.vue";

const props = defineProps<{ content: IConfigurablePages }>();
</script>

<template>
  <template v-for="orderedBlock in content.blockOrder" :key="orderedBlock.id">
    <PageBanner
      v-if="orderedBlock.block.mg_tableclass === 'cms.Headers'"
      :id="orderedBlock.block.id"
      :title="orderedBlock.block.title"
      :subtitle="orderedBlock.block.subtitle"
      :background-image="orderedBlock.block.backgroundImage?.image?.url"
      :enable-full-screen-width="orderedBlock.block.enableFullScreenWidth"
      :title-is-centered="orderedBlock.block.titleIsCentered"
    />
    <PageSection
      v-else-if="orderedBlock.block.mg_tableclass === 'cms.Sections'"
      :id="orderedBlock.block.id"
      :enable-full-screen-width="orderedBlock.block.enableFullScreenWidth"
    >
      <template
        v-for="orderedComponent in orderedBlock.block.componentOrder"
        :key="orderedComponent.id"
      >
        <PageComponent
          :mg_tableclass="orderedComponent.component.mg_tableclass"
          :component="orderedComponent.component"
        />
      </template>
    </PageSection>
    <div v-else>
      <TextParagraph id="block-does-not-exist-message">
        Block {{ orderedBlock.block.mg_tableclass }} is not yet supported.
      </TextParagraph>
    </div>
  </template>
</template>
