<script setup lang="ts">
import { ref, computed } from "vue";

import Banner from "./Banner.vue";
import Section from "./Section.vue";
import Heading from "./Heading.vue";
import Paragraph from "./Paragraph.vue";
import Image from "../pages/Image.vue";
import NavigationGroups from "./Navigation/NavigationGroups.vue";

import EditModal from "../form/EditModal.vue";

import { parsePageText } from "../../utils/cms";
import type { IPageComponent } from "../../../types/CmsComponents";
import type { ITableMetaData } from "../../../../metadata-utils/src";

const props = withDefaults(
  defineProps<{
    component: IPageComponent;
    mg_tableclass: string;
    metadata?: ITableMetaData[];
    isEditable?: boolean;
  }>(),
  {
    isEditable: false,
  }
);

const showEditModal = ref<boolean>(false);
const editingIsEnabled = computed<boolean>(() => {
  return props.isEditable && typeof componentMetadata !== "undefined";
});

const schemaTableName = ref<string>(
  props.mg_tableclass.split(".")[1] as string
);
const componentData = ref<IPageComponent>(props.component);
if (
  !props.mg_tableclass.endsWith(".Images") &&
  Object.keys(props.component).includes("image")
) {
  delete props.component["image" as keyof IPageComponent];
}

const componentMetadata = computed<ITableMetaData | undefined>(() => {
  if (props.metadata) {
    return props.metadata.filter(
      (table) => table.name === schemaTableName.value
    )[0] as ITableMetaData;
  }
  return undefined;
});

function onEdit(component?: string, value?: IPageComponent) {
  if (component) {
    schemaTableName.value = component as string;
  }

  if (value) {
    componentData.value = value as IPageComponent;
  }

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
    :is-editable="editingIsEnabled"
    @edit="onEdit"
  />
  <Paragraph
    v-else
    id="component-does-not-exist-message"
    :text="`Component ${mg_tableclass} is not yet supported`"
  />
  <EditModal
    v-if="componentMetadata && showEditModal"
    :key="`edit-modal-${componentMetadata.id}`"
    :showButton="false"
    :schemaId="componentMetadata.schemaId"
    :metadata="componentMetadata"
    :formValues="(componentData as Record<string,any>)"
    :isInsert="false"
    v-model:visible="showEditModal"
  />
</template>
