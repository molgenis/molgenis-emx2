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

import { deleteBlock, deleteComponent, parsePageText } from "../../utils/cms";
import type { IPageComponent } from "../../../types/CmsComponents";
import type { ITableMetaData } from "../../../../metadata-utils/src";
import type { IComponentOrders } from "../../..//types/cms";

const props = withDefaults(
  defineProps<{
    component: IPageComponent;
    componentType: string;
    mg_tableclass: string;
    metadata?: ITableMetaData[];
    isEditable?: boolean;
    parent: string;
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
  console.log("DELETE", props.component);
  if (props.componentType === "Component") {
    deleteComponent(
      componentMetadata.value?.schemaId || "",
      componentData.value.id,
      props.parent
    );
  } else {
    deleteBlock(
      componentMetadata.value?.schemaId || "",
      componentData.value.id,
      props.parent
    );
  }
  emit("updatePage");
}

const menuPlacement = computed<string>(() => {
  let placement = "top-start";
  const component = componentData;
  if (
    props.component?.headingIsCentered ||
    props.component?.paragraphIsCentered ||
    props.component?.imageIsCentered
  ) {
    placement = "top";
  }
  return placement;
});
</script>

<template>
  <VMenu
    :placement="menuPlacement"
    :disabled="!isEditable"
    v-model:shown="showMenu"
    show-group="component-menu"
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
      :id="componentData.id"
      :title="componentData.title"
      :subtitle="componentData.subtitle"
      :background-image="componentData.backgroundImage?.image?.url"
      :enable-full-screen-width="componentData.enableFullScreenWidth"
      :title-is-centered="componentData.titleIsCentered"
      :is-editable="editingIsEnabled"
    />
    <Section
      v-else-if="mg_tableclass.endsWith('.Sections')"
      v-model:showMenu="showMenu"
      :id="componentData.id"
      :enable-full-screen-width="componentData.enableFullScreenWidth"
    >
      <slot></slot>
    </Section>
    <Heading
      v-else-if="mg_tableclass.endsWith('.Headings')"
      v-model:showMenu="showMenu"
      :id="componentData.id"
      :heading-is-centered="componentData.headingIsCentered"
      :level="componentData.level"
      class="mb-5"
      :text="parsePageText(componentData.text)"
      :is-editable="editingIsEnabled"
    />
    <Paragraph
      v-else-if="mg_tableclass.endsWith('.Paragraphs')"
      v-model:showMenu="showMenu"
      :id="componentData.id"
      :paragraph-is-centered="componentData.paragraphIsCentered"
      class="mb-2.5 last:mb-0"
      :text="parsePageText(componentData.text)"
      :is-editable="editingIsEnabled"
    />
    <Image
      v-else-if="mg_tableclass.endsWith('.Images')"
      v-model:showMenu="showMenu"
      :id="componentData.id"
      :image="componentData.image"
      :width="componentData.width"
      :height="componentData.height"
      :alt="componentData.alt"
      :image-is-centered="componentData.imageIsCentered"
      :is-editable="editingIsEnabled"
    />
    <NavigationGroups
      v-else-if="mg_tableclass.endsWith('.Navigation groups')"
      v-model:showMenu="showMenu"
      :id="componentData.id"
      :links="componentData.links"
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
