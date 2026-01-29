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

import { parsePageText, sortConfigurablePage } from "../../utils/Pages";

const props = defineProps<{ content: IConfigurablePages }>();
const page = sortConfigurablePage(props.content);
</script>

<template>
  <template v-for="block in (page.blocks as IHeaders[])">
    <PageBanner
      v-if="block.mg_tableclass === 'cms.Headers'"
      :id="block.id"
      :title="block.title"
      :subtitle="block.subtitle"
      :background-image="block.backgroundImage?.image?.url"
      :enable-full-screen-width="block.enableFullScreenWidth"
      :title-is-centered="block.titleIsCentered"
    />
    <PageSection
      v-else-if="block.mg_tableclass === 'cms.Sections'"
      :id="block.id"
      :enable-full-screen-width="block.enableFullScreenWidth"
    >
      <template v-for="component in (block.components as BlockComponent[])">
        <TextHeading
          v-if="component.mg_tableclass === 'cms.Headings'"
          :id="component.id"
          :heading-is-centered="component.headingIsCentered"
          :level="component.level"
          class="mb-5"
        >
          {{ parsePageText(component.text as string) }}
        </TextHeading>
        <TextParagraph
          v-else-if="component.mg_tableclass === 'cms.Paragraphs'"
          :id="component.id"
          :paragraph-is-centered="component.paragraphIsCentered"
          class="mb-2.5 last:mb-0"
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
