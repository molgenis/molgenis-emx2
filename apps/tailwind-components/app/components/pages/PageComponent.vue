<script setup lang="ts">
import { ref, computed } from "vue";

import Banner from "./Banner.vue";
import Section from "./Section.vue";
import Heading from "./Heading.vue";
import Paragraph from "./Paragraph.vue";
import Image from "../pages/Image.vue";
import NavigationGroups from "./Navigation/NavigationGroups.vue";
import NavigationCards from "./Navigation/NavigationCards.vue";

import Button from "../Button.vue";

import { parsePageText } from "../../utils/cms";
import type { IPageComponent } from "../../../types/CmsComponents";
import type { ITableMetaData } from "../../../../metadata-utils/src";

const props = defineProps<{
  component: IPageComponent;
  mg_tableclass: string;
  metadata?: ITableMetaData[];
  isEditable?: boolean;
}>();

const componentMetadata = computed<ITableMetaData | undefined>(() => {
  if (props.metadata) {
    const schemaTableName = props.mg_tableclass.split(".")[1];
    return props.metadata.filter((table) => table.id === schemaTableName)[0];
  }
  return undefined;
});
</script>

<template>
  <Banner
    v-if="mg_tableclass === 'cms.Headers'"
    :id="component.id"
    :title="component.title"
    :subtitle="component.subtitle"
    :background-image="component.backgroundImage?.image?.url"
    :enable-full-screen-width="component.enableFullScreenWidth"
    :title-is-centered="component.titleIsCentered"
    :is-editable="isEditable"
    :class="{
      group: isEditable && componentMetadata,
    }"
  >
    <Button
      v-if="isEditable"
      class="absolute hidden group-hover:inline-flex top-5 right-5"
      iconOnly
      icon="edit"
      type="secondary"
      label="Edit header"
      size="small"
    />
  </Banner>
  <Section
    v-else-if="mg_tableclass === 'cms.Sections'"
    :id="component.id"
    :enable-full-screen-width="component.enableFullScreenWidth"
  >
    <slot></slot>
  </Section>
  <Heading
    v-else-if="mg_tableclass === 'cms.Headings'"
    :id="component.id"
    :heading-is-centered="component.headingIsCentered"
    :level="component.level"
    class="mb-5 relative"
    :class="{
      group: isEditable && componentMetadata,
    }"
    :text="parsePageText(component.text)"
  >
    <Button
      v-if="isEditable"
      class="absolute hidden group-hover:inline-flex h-auto w-auto bottom-1 ml-1"
      iconOnly
      icon="edit"
      type="secondary"
      label="Edit heading"
      size="small"
    />
  </Heading>
  <Paragraph
    v-else-if="mg_tableclass === 'cms.Paragraphs'"
    :id="component.id"
    :paragraph-is-centered="component.paragraphIsCentered"
    class="mb-2.5 last:mb-0 relative"
    :class="{
      group: isEditable && componentMetadata,
    }"
    :text="parsePageText(component.text)"
  >
    <Button
      v-if="isEditable"
      class="absolute hidden group-hover:inline-flex h-auto w-auto -bottom-1 ml-1"
      iconOnly
      icon="edit"
      type="secondary"
      label="Edit paragraph"
      size="small"
    />
  </Paragraph>
  <Image
    v-else-if="mg_tableclass === 'cms.Images'"
    :id="component.id"
    :image="component.image"
    :width="component.width"
    :height="component.height"
    :alt="component.alt"
    :image-is-centered="component.imageIsCentered"
  >
    <Button
      v-if="isEditable"
      class="absolute hidden group-hover:inline-flex h-auto w-auto -bottom-1 ml-1"
      iconOnly
      icon="edit"
      type="secondary"
      label="Edit image"
      size="small"
    />
  </Image>
  <NavigationGroups
    v-else-if="mg_tableclass === 'cms.Navigation groups'"
    :id="component.id"
  >
    <template #links>
      <NavigationCards
        v-for="card in component.links"
        :key="card.id"
        :id="card.id"
        :title="card.title"
        :description="card.description"
        :url="card.url"
        :url-is-external="card.urlIsExternal"
        :url-label="card.urlLabel"
        class="relative w-full md:w-80"
        :class="{
          group: isEditable && componentMetadata,
        }"
      >
        <Button
          v-if="isEditable"
          class="absolute hidden group-hover:inline-flex top-1 right-1"
          iconOnly
          icon="edit"
          type="secondary"
          label="Edit card"
          size="small"
        />
      </NavigationCards>
    </template>
  </NavigationGroups>
  <Paragraph v-else id="component-does-not-exist-message">
    Component {{ mg_tableclass }} is not yet supported
  </Paragraph>
</template>
