<script setup lang="ts">
import type { IConfigurablePages } from "../../../types/cms";

import PageBanner from "../pages/Banner.vue";
import PageSection from "../pages/Section.vue";
import TextHeading from "../text/Heading.vue";
import TextParagraph from "../text/Paragraph.vue";
import Image from "../pages/Image.vue";

import { parsePageText } from "../../utils/Pages";

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
      :background-color="orderedBlock.block.backgroundColor"
    >
      <template
        v-for="orderedComponent in orderedBlock.block.componentOrder"
        :key="orderedComponent.id"
      >
        <TextHeading
          v-if="orderedComponent.component.mg_tableclass === 'cms.Headings'"
          :id="orderedComponent.component.id"
          :heading-is-centered="orderedComponent.component.headingIsCentered"
          :level="orderedComponent.component.level"
          :heading-is-hidden="orderedComponent.component.headingIsHidden"
          class="mb-5"
        >
          {{ parsePageText(orderedComponent.component.text) }}
        </TextHeading>
        <TextParagraph
          v-else-if="
            orderedComponent.component.mg_tableclass === 'cms.Paragraphs'
          "
          :id="orderedComponent.component.id"
          :paragraph-is-centered="
            orderedComponent.component.paragraphIsCentered
          "
          class="mb-2.5 last:mb-0"
        >
          {{ parsePageText(orderedComponent.component.text) }}
        </TextParagraph>
        <Image
          v-else-if="orderedComponent.component.mg_tableclass === 'cms.Images'"
          :id="orderedComponent.component.id"
          :image="orderedComponent.component.image"
          :width="orderedComponent.component.width"
          :height="orderedComponent.component.height"
          :alt="orderedComponent.component.alt"
          :image-is-centered="orderedComponent.component.imageIsCentered"
        />
        <div v-else>
          <TextParagraph id="component-does-not-exist-message">
            Component {{ orderedComponent.component.mg_tableclass }} is not yet
            supported.
          </TextParagraph>
        </div>
      </template>
    </PageSection>
    <div v-else>
      <TextParagraph id="block-does-not-exist-message">
        Block {{ orderedBlock.block.mg_tableclass }} is not yet supported.
      </TextParagraph>
    </div>
  </template>
</template>
