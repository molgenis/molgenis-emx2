<script setup lang="ts">
import Heading from "./Heading.vue";
import Paragraph from "./Paragraph.vue";
import Image from "../pages/Image.vue";
import NavigationGroups from "./Navigation/NavigationGroups.vue";

import { parsePageText } from "../../utils/cms";
import type { IPageComponent } from "../../../types/CmsComponents";

const props = withDefaults(
  defineProps<{
    component: IPageComponent;
    mg_tableclass: string;
    isEditable?: boolean;
  }>(),
  {
    isEditable: false,
  }
);
</script>

<template>
  <Heading
    v-if="mg_tableclass === 'cms.Headings'"
    :id="component.id"
    :heading-is-centered="component.headingIsCentered"
    :level="component.level"
    class="mb-5"
  >
    {{ parsePageText(component.text) }}
  </Heading>
  <Paragraph
    v-else-if="mg_tableclass === 'cms.Paragraphs'"
    :id="component.id"
    :paragraph-is-centered="component.paragraphIsCentered"
    class="mb-2.5 last:mb-0"
  >
    {{ parsePageText(component.text) }}
  </Paragraph>
  <Image
    v-else-if="mg_tableclass === 'cms.Images'"
    :id="component.id"
    :image="component.image"
    :width="component.width"
    :height="component.height"
    :alt="component.alt"
    :image-is-centered="component.imageIsCentered"
  />
  <NavigationGroups
    v-else-if="mg_tableclass === 'cms.Navigation groups'"
    :id="component.id"
    :links="component.links"
  />
  <div v-else>
    <Paragraph id="component-does-not-exist-message">
      Component {{ mg_tableclass }} is not yet supported
    </Paragraph>
  </div>
</template>
