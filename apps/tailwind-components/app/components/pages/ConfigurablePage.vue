<script setup lang="ts">
import type {
  IConfigurablePages,
  IComponents,
  IHeaders,
  IHeadings,
  IParagraphs,
  IImages,
} from "../../../types/cms";

interface BlockComponent extends IComponents, IHeadings, IParagraphs, IImages {}

import PageBanner from "../pages/Banner.vue";
import PageSection from "../pages/Section.vue";
import TextHeading from "../text/Heading.vue";
import TextParagraph from "../text/Paragraph.vue";
import Image from "../Image.vue";

import { parsePageText } from "../../utils/Pages";

const props = defineProps<{ content: IConfigurablePages }>();
</script>

<template>
  <template v-for="block in (content.blocks as IHeaders[])">
    <PageBanner
      v-if="block.mg_tableclass === 'cms.Headers'"
      :id="block.id"
      :title="block.title"
      :subtitle="block.subtitle"
      :background-image="block.backgroundImage?.image?.url"
    />
    <PageSection
      v-else-if="block.mg_tableclass === 'cms.Sections'"
      :id="block.id"
    >
      <template v-for="component in (block.components as BlockComponent[])">
        <TextHeading
          v-if="component.mg_tableclass === 'cms.Headings'"
          :id="component.id"
          :is-centered="component.headingIsCentered"
          :level="component.level"
        >
          {{ parsePageText(component.text as string) }}
        </TextHeading>
        <TextParagraph
          v-else-if="component.mg_tableclass === 'cms.Paragraphs'"
          :id="component.id"
          :is-centered="component.paragraphIsCentered"
        >
          {{ parsePageText(component.text as string) }}
        </TextParagraph>
        <Image
          v-else-if="component.mg_tableclass === 'cms.Images'"
          :id="component.id"
          :src="(component.image?.url as string)"
          :width="component.width"
          :height="component.height"
          :alt="component.alt"
        />
        <div v-else>
          <TextParagraph id="component-does-not-exist-message">
            Component {{ component.mg_tableclass }} is not yet supported.
          </TextParagraph>
        </div>
      </template>
    </PageSection>
  </template>
</template>
