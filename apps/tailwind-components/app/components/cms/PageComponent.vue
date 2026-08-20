<script setup lang="ts">
import { ref, computed } from "vue";

import Banner from "./Banner.vue";
import Section from "./Section.vue";
import Heading from "./Heading.vue";
import Paragraph from "./Paragraph.vue";
import Image from "./Image.vue";
import NavigationGroups from "./Navigation/NavigationGroups.vue";

import EditModal from "../form/EditModal.vue";

import {
  deleteBlock,
  deleteComponent,
  parsePageText,
  moveComponentUp,
  moveBlockUp,
  moveComponentDown,
  moveBlockDown,
} from "../../utils/cms";
import type { IFile } from "../../../types/cms";
import type { IPageComponent } from "../../../types/CmsComponents";
import type { ITableMetaData } from "../../../../metadata-utils/src";

const props = withDefaults(
  defineProps<{
    component: IPageComponent;
    orderId: string;
    order?: number;
    componentType: string;
    mg_tableclass: string;
    metadata?: ITableMetaData[];
    isEditable?: boolean;
    parent: string;
  }>(),
  {
    isEditable: false,
    order: 0,
  }
);

const emit = defineEmits(["updatePage", "dragging"]);
const showEditModal = ref<boolean>(false);
const showDeleteModal = ref<boolean>(false);
const currentlyDeleting = ref<boolean>(false);
const editingIsEnabled = computed<boolean>(() => {
  return props.isEditable && componentMetadata.value !== undefined;
});

const schemaTableName = ref<string>(
  props.mg_tableclass.split(".")[1] as string
);

const componentData = ref<IPageComponent>(props.component);
const headerComponentImage = ref<IFile>();

if (
  props.mg_tableclass.endsWith(".Headers") &&
  Object.keys(componentData.value).includes("backgroundImage")
) {
  headerComponentImage.value = componentData.value.backgroundImage.image;
  componentData.value.backgroundImage = {
    id: componentData.value.backgroundImage.id,
  };
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

async function doDelete(): Promise<void> {
  currentlyDeleting.value = true;
  if (props.componentType === "Component") {
    await deleteComponent(
      componentMetadata.value?.schemaId || "",
      props.component.id,
      props.orderId,
      props.parent
    );
  } else {
    await deleteBlock(
      componentMetadata.value?.schemaId || "",
      props.component.id,
      props.orderId,
      props.parent
    );
  }
  currentlyDeleting.value = false;
  showDeleteModal.value = false;
  emit("updatePage");
}

async function handleMoveEvent(action: "up" | "down" | "grab" | "release") {
  if (action === "grab" || action === "release") {
    emit("dragging", {
      dragging: action === "grab",
      componentType: props.componentType,
      componentName: props.mg_tableclass.split(".")[1],
      action: "move",
      moveOrderId: props.orderId,
      parentId: props.parent,
    });
    return;
  }
  if (action === "up") {
    if (props.componentType === "Component") {
      await moveComponentUp(
        componentMetadata.value?.schemaId || "",
        props.orderId,
        props.order,
        props.parent
      );
    } else {
      await moveBlockUp(
        componentMetadata.value?.schemaId || "",
        props.orderId,
        props.order,
        props.parent
      );
    }
    emit("updatePage");
  }
  if (action === "down") {
    if (props.componentType === "Component") {
      await moveComponentDown(
        componentMetadata.value?.schemaId || "",
        props.orderId,
        props.order,
        props.parent
      );
    } else {
      await moveBlockDown(
        componentMetadata.value?.schemaId || "",
        props.orderId,
        props.order,
        props.parent
      );
    }
    emit("updatePage");
  }
}
</script>

<template>
  <Banner
    v-if="mg_tableclass.endsWith('.Headers')"
    :id="component.id"
    :title="component.title"
    :subtitle="component.subtitle"
    :background-image="component.backgroundImage"
    :image="headerComponentImage"
    :enable-full-screen-width="component.enableFullScreenWidth"
    :title-is-centered="component.titleIsCentered"
    :isEditable="editingIsEnabled"
    @edit="showEditModal = true"
    @delete="onDelete"
    @move="handleMoveEvent"
  />
  <Section
    v-else-if="mg_tableclass.endsWith('.Sections')"
    :id="component.id"
    :enable-full-screen-width="component.enableFullScreenWidth"
    :isEditable="editingIsEnabled"
    @edit="showEditModal = true"
    @delete="onDelete"
    @move="handleMoveEvent"
  >
    <slot></slot>
  </Section>
  <Heading
    v-else-if="mg_tableclass.endsWith('.Headings')"
    :id="component.id"
    :heading-is-centered="component.headingIsCentered"
    :level="component.level"
    class="mb-5"
    :text="parsePageText(component.text)"
    :isEditable="editingIsEnabled"
    @edit="showEditModal = true"
    @delete="onDelete"
    @move="handleMoveEvent"
  />
  <Paragraph
    v-else-if="mg_tableclass.endsWith('.Paragraphs')"
    class="mb-2.5 last:mb-0"
    :id="component.id"
    :paragraph-is-centered="component.paragraphIsCentered"
    :text="parsePageText(component.text)"
    :isEditable="editingIsEnabled"
    @edit="showEditModal = true"
    @delete="onDelete"
    @move="handleMoveEvent"
  />
  <Image
    v-else-if="mg_tableclass.endsWith('.Images')"
    :id="component.id"
    :image="component.image"
    :width="component.width"
    :height="component.height"
    :alt="component.alt"
    :image-is-centered="component.imageIsCentered"
    :isEditable="editingIsEnabled"
    @edit="showEditModal = true"
    @delete="onDelete"
    @move="handleMoveEvent"
  />
  <NavigationGroups
    v-else-if="mg_tableclass.endsWith('.Navigation groups')"
    :id="component.id"
    :links="component.links"
    :isEditable="editingIsEnabled"
  />
  <Paragraph
    v-else
    id="component-does-not-exist-message"
    name="Error"
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
    @update:updated="
      $emit('updatePage');
      showEditModal = false;
    "
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
          <Button
            v-if="!currentlyDeleting"
            type="outline"
            @click="showDeleteModal = false"
          >
            Cancel
          </Button>
          <Button
            v-if="!currentlyDeleting"
            icon="trash"
            type="primary"
            @click="doDelete"
          >
            Delete
          </Button>
          <Button v-if="currentlyDeleting" type="primary" disabled>
            Deleting...
          </Button>
        </div>
      </menu>
    </template>
  </Modal>
</template>
