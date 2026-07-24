<script setup lang="ts">
import { ref, computed } from "vue";

import Banner from "./Banner.vue";
import Section from "./Section.vue";
import Heading from "./Heading.vue";
import Paragraph from "./Paragraph.vue";
import Image from "../pages/Image.vue";
import NavigationGroups from "./Navigation/NavigationGroups.vue";
import ComponentActions from "./ComponentActions.vue";

import EditModal from "../form/EditModal.vue";

import { parsePageText } from "../../utils/cms";
import type { IPageComponent } from "../../../types/CmsComponents";
import type { ITableMetaData } from "../../../../metadata-utils/src";
import DeleteModal from "../form/DeleteModal.vue";

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

const emit = defineEmits(["updatePage"]);
const showMenu = ref<boolean>(false);

const showEditModal = ref<boolean>(false);

const editingIsEnabled = computed<boolean>(() => {
  return props.isEditable && componentMetadata.value !== undefined;
});

const schemaTableName = ref<string>(
  props.mg_tableclass.split(".")[1] as string
);

const componentData = ref<IPageComponent>(props.component);
// temporary workaround for graphql bug #2706
if (
  !props.mg_tableclass.endsWith(".Images") &&
  Object.keys(componentData.value).includes("image")
) {
  delete componentData.value["image" as keyof IPageComponent];
}

const componentMetadata = computed<ITableMetaData | undefined>(() => {
  if (props.metadata) {
    return props.metadata.filter(
      (table) => table.name === schemaTableName.value
    )[0] as ITableMetaData;
  }
  return undefined;
});

function onDelete() {
  // TODO:
  // confirm if user is sure!
  // are we deleting a block or component?
  // if component remove from correct table and from order table
  // recalculate component order
  // if block
  // delete all containing components & orders
  // recalculate block order
}

// Todo: some situations need different menu placements
const menuPlacement = computed<string>(() => {
  let placement = 'top-start';
  if(props.component?.headingIsCentered || props.component?.paragraphIsCentered || props.component?.imageIsCentered){
    placement = 'top';
  }
  return placement
});

</script>

<template>
  <VMenu :placement="menuPlacement" :disabled="!isEditable" v-model:shown="showMenu" show-group="component-menu"
    :triggers="['hover', 'focus']"
  :popper-triggers="['hover', 'focus']"
  :delay="{ show: 100, hide: 200 }"
  >
    <template #popper>
      <ComponentActions
        v-if="isEditable"
        :name="componentMetadata?.name"
        @edit="showEditModal = true"
        @delete="onDelete"
      />
    </template>

    <Banner
      v-if="mg_tableclass.endsWith('.Headers')"
      v-model:showMenu="showMenu"
      :id="component.id"
      :title="component.title"
      :subtitle="component.subtitle"
      :background-image="component.backgroundImage?.image?.url"
      :enable-full-screen-width="component.enableFullScreenWidth"
      :title-is-centered="component.titleIsCentered"
      :is-editable="editingIsEnabled"
    />
    <Section
      v-else-if="mg_tableclass.endsWith('.Sections')"
      v-model:showMenu="showMenu"
      :id="component.id"
      :enable-full-screen-width="component.enableFullScreenWidth"
    >
      <slot></slot>
    </Section>
    <Heading
      v-else-if="mg_tableclass.endsWith('.Headings')"
      v-model:showMenu="showMenu"
      :id="component.id"
      :heading-is-centered="component.headingIsCentered"
      :level="component.level"
      class="mb-5"
      :text="parsePageText(component.text)"
      :is-editable="editingIsEnabled"
    />
    <Paragraph
      v-else-if="mg_tableclass.endsWith('.Paragraphs')"
      v-model:showMenu="showMenu"
      :id="component.id"
      :paragraph-is-centered="component.paragraphIsCentered"
      class="mb-2.5 last:mb-0"
      :text="parsePageText(component.text)"
      :is-editable="editingIsEnabled"
    />
    <Image
      v-else-if="mg_tableclass.endsWith('.Images')"
      v-model:showMenu="showMenu"
      :id="component.id"
      :image="component.image"
      :width="component.width"
      :height="component.height"
      :alt="component.alt"
      :image-is-centered="component.imageIsCentered"
      :is-editable="editingIsEnabled"
    />
    <NavigationGroups
      v-else-if="mg_tableclass.endsWith('.Navigation groups')"
      v-model:showMenu="showMenu"
      :id="component.id"
      :links="component.links"
      :is-editable="editingIsEnabled"
    />
    <Paragraph
      v-else
      id="component-does-not-exist-message"
      :text="`Component ${mg_tableclass} is not yet supported`"
    />
  </VMenu>

  <EditModal
    v-if="componentMetadata && showEditModal"
    :key="`edit-modal-${componentMetadata.id}`"
    :showButton="false"
    :schemaId="componentMetadata.schemaId"
    :metadata="componentMetadata"
    :formValues="(componentData as Record<string,any>)"
    :isInsert="false"
    @update:updated="$emit('updatePage')"
    v-model:visible="showEditModal"
  />
</template>
