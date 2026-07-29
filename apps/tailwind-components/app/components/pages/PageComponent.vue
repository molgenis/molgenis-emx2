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
const showDeleteModal = ref<boolean>(false);

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
  showDeleteModal.value = true;
}

function doDelete() {
  showDeleteModal.value = false;
  if (props.componentType === "Component") {
    deleteComponent(
      componentMetadata.value?.schemaId || "",
      props.component.id,
      props.parent
    );
  } else {
    deleteBlock(
      componentMetadata.value?.schemaId || "",
      props.component.id,
      props.parent
    );
  }
  emit("updatePage");
}

const menuPlacement = computed<string>(() => {
  let placement = "top-start";
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
    @update:updated="$emit('updatePage'); showEditModal = false"
    v-model:visible="showEditModal"
  />

  <Modal
    v-model:visible="showDeleteModal"
    title="Delete"
    :subtitle="`${componentMetadata?.name}`"
  >
    <p class="p-8">Are you sure you want to delete this component?</p>
    <template #footer>
      <menu class="flex items-center justify-end h-[116px]">
        <div class="flex gap-4">
          <Button type="outline" @click="showDeleteModal = false">
            Cancel
          </Button>
          <Button icon="trash" type="primary" @click="doDelete">
            Delete
          </Button>
        </div>
      </menu>
    </template>
  </Modal>
</template>
