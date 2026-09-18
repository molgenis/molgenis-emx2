<script setup lang="ts">
import { ref, computed } from "vue";
import { hideAllPoppers } from "floating-vue";

import Paragraph from "./paragraph/Paragraph.vue";
import EditableHeader from "./header/EditableHeader.vue";
import EditableSection from "./section/EditableSection.vue";
import EditableHeading from "./heading/EditableHeading.vue";
import EditableParagraph from "./paragraph/EditableParagraph.vue";
import EditableImage from "./image/EditableImage.vue";
import EditableOrderedList from "./lists/EditableOrderedList.vue";
import EditableUnorderedList from "./lists/EditableUnorderedList.vue";
import EditableNavigationCard from "./navigationCard/EditableNavigationCard.vue";

import EditModal from "../form/EditModal.vue";

import {
  deleteBlock,
  deleteComponent,
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
    page: string;
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

const headerComponentImage = ref<IFile>();
const formComponentData = computed<IPageComponent>(() => props.component);
const componentMetadata = computed<ITableMetaData | undefined>(() => {
  if (props.metadata) {
    return props.metadata.filter(
      (table) => table.name === schemaTableName.value
    )[0] as ITableMetaData;
  }
  return undefined;
});

// this is required to flatten the File type and preserve the component-image link
if (
  props.mg_tableclass.endsWith(".Headers") &&
  Object.keys(formComponentData.value).includes("backgroundImage")
) {
  headerComponentImage.value = formComponentData.value.backgroundImage.image;
  formComponentData.value.backgroundImage = {
    id: formComponentData.value.backgroundImage.id,
  };
}

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
  hideAllPoppers();
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
        props.parent,
        props.page
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
        props.parent,
        props.page
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
  hideAllPoppers();
}

function onShowEdit() {
  showEditModal.value = true;
  hideAllPoppers();
}

function onEdited() {
  showEditModal.value = false;
  hideAllPoppers();
  emit("updatePage");
}

function asSingularName(value: string | undefined): string | undefined {
  if (value && value !== "" && value.toLowerCase().endsWith("s")) {
    return value.slice(0, value.length - 1).toLowerCase();
  }
  return value;
}
</script>

<template>
  <EditableHeader
    v-if="mg_tableclass.endsWith('.Headers')"
    v-bind="component"
    :image="headerComponentImage"
    :isEditable="editingIsEnabled"
    @edit="onShowEdit"
    @delete="onDelete"
    @move="handleMoveEvent"
  />
  <EditableSection
    v-else-if="mg_tableclass.endsWith('.Sections')"
    v-bind="component"
    :isEditable="editingIsEnabled"
    @edit="onShowEdit"
    @delete="onDelete"
    @move="handleMoveEvent"
  >
    <slot></slot>
  </EditableSection>
  <EditableHeading
    v-else-if="mg_tableclass.endsWith('.Headings')"
    v-bind="component"
    :isEditable="editingIsEnabled"
    @edit="onShowEdit"
    @delete="onDelete"
    @move="handleMoveEvent"
  />
  <EditableParagraph
    v-else-if="mg_tableclass.endsWith('.Paragraphs')"
    v-bind="component"
    :isEditable="editingIsEnabled"
    @edit="onShowEdit"
    @delete="onDelete"
    @move="handleMoveEvent"
  />
  <EditableImage
    v-else-if="mg_tableclass.endsWith('.Images')"
    v-bind="component"
    :isEditable="editingIsEnabled"
    @edit="onShowEdit"
    @delete="onDelete"
    @move="handleMoveEvent"
  />
  <EditableNavigationCard
    v-else-if="mg_tableclass.endsWith('.Navigation cards')"
    v-bind="component"
    :isEditable="editingIsEnabled"
    @edit="onShowEdit"
    @delete="onDelete"
    @move="handleMoveEvent"
  />
  <EditableOrderedList
    v-else-if="mg_tableclass.endsWith('.Ordered lists')"
    v-bind="component"
    :isEditable="editingIsEnabled"
    @edit="onShowEdit"
    @delete="onDelete"
    @move="handleMoveEvent"
  />
  <EditableUnorderedList
    v-else-if="mg_tableclass.endsWith('.Unordered lists')"
    v-bind="component"
    :isEditable="editingIsEnabled"
    @edit="onShowEdit"
    @delete="onDelete"
    @move="handleMoveEvent"
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
    :formValues="(formComponentData as Record<string,any>)"
    :isInsert="false"
    @update:updated="onEdited"
    v-model:visible="showEditModal"
  />
  <Modal
    v-model:visible="showDeleteModal"
    :title="`Delete ${asSingularName(componentMetadata?.name as string)}?`"
    size="medium"
  >
    <div class="p-8 text-title-contrast">
      <p class="mb-1 font-bold">
        Are you sure you want to delete this
        {{ asSingularName(componentMetadata?.name) }}?
      </p>
      <p
        v-if="['Sections'].includes(componentMetadata?.name as string)"
        class="mb-1"
      >
        By deleting this component, all other linked components or files linked
        will be removed.
      </p>
      <p>This action cannot be undone.</p>
    </div>
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
