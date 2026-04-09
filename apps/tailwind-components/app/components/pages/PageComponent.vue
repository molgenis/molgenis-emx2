<script setup lang="ts">
import { ref, computed } from "vue";

import Banner from "./Banner.vue";
import Section from "./Section.vue";
import Heading from "./Heading.vue";
import Paragraph from "./Paragraph.vue";
import Image from "../pages/Image.vue";
import NavigationGroups from "./Navigation/NavigationGroups.vue";
import NavigationCards from "./Navigation/NavigationCards.vue";

import EditModal from "../form/EditModal.vue";

import { parsePageText } from "../../utils/cms";
import type { IPageComponent } from "../../../types/CmsComponents";
import type { ITableMetaData } from "../../../../metadata-utils/src";

const props = defineProps<{
  component: IPageComponent;
  mg_tableclass: string;
  metadata?: ITableMetaData[];
  isEditable?: boolean;
}>();

const showEditModal = ref<boolean>(false);

const editingIsEnabled = computed<boolean>(() => {
  return props.isEditable && typeof componentMetadata !== "undefined";
});

const componentMetadata = computed<ITableMetaData | undefined>(() => {
  if (props.metadata) {
    const schemaTableName = props.mg_tableclass.split(".")[1];
    return props.metadata.filter((table) => table.id === schemaTableName)[0];
  }
  return undefined;
});

function onEdit() {
  showEditModal.value = true;
}
</script>

<template>
  <Banner
    v-if="mg_tableclass.endsWith('.Headers')"
    :id="component.id"
    :title="component.title"
    :subtitle="component.subtitle"
    :background-image="component.backgroundImage?.image?.url"
    :enable-full-screen-width="component.enableFullScreenWidth"
    :title-is-centered="component.titleIsCentered"
    :is-editable="editingIsEnabled"
    @edit="onEdit"
  />
  <Section
    v-else-if="mg_tableclass.endsWith('.Sections')"
    :id="component.id"
    :enable-full-screen-width="component.enableFullScreenWidth"
  >
    <slot></slot>
  </Section>
  <Heading
    v-else-if="mg_tableclass.endsWith('.Headings')"
    :id="component.id"
    :heading-is-centered="component.headingIsCentered"
    :level="component.level"
    class="mb-5 inline-flex"
    :text="parsePageText(component.text)"
    :is-editable="editingIsEnabled"
    @edit="onEdit"
  />
  <Paragraph
    v-else-if="mg_tableclass.endsWith('.Paragraphs')"
    :id="component.id"
    :paragraph-is-centered="component.paragraphIsCentered"
    class="mb-2.5 last:mb-0"
    :text="parsePageText(component.text)"
    :is-editable="editingIsEnabled"
    @edit="onEdit"
  />
  <Image
    v-else-if="mg_tableclass.endsWith('.Images')"
    :id="component.id"
    :image="component.image"
    :width="component.width"
    :height="component.height"
    :alt="component.alt"
    :image-is-centered="component.imageIsCentered"
    :is-editable="editingIsEnabled"
    @edit="onEdit"
  />
  <NavigationGroups
    v-else-if="mg_tableclass.endsWith('.Navigation groups')"
    :id="component.id"
    :links="component.links"
  />
  <Paragraph v-else id="component-does-not-exist-message">
    Component {{ mg_tableclass }} is not yet supported
  </Paragraph>
  <EditModal
    v-if="componentMetadata && showEditModal"
    :key="`edit-modal-${componentMetadata.id}`"
    :showButton="false"
    :schemaId="componentMetadata.schemaId"
    :metadata="componentMetadata"
    :formValues="(component as Record<string,any>)"
    :isInsert="false"
    v-model:visible="showEditModal"
  />
</template>
