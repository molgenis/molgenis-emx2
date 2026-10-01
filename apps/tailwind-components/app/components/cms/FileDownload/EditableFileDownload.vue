<script setup lang="ts">
import { ref } from "vue";
import type { IFileLists } from "../../../../types/cms";
import { UploadFile, AddLink } from "../../../utils/cms/add";
import ComponentActions from "../ComponentActions.vue";
import FileDownload from "./FileDownload.vue";
import Button from "../../Button.vue";
import { useForm } from "#imports";
import type {
  ColumnType,
  ITableMetaData,
} from "../../../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<IFileLists & { isEditable?: boolean; schema: string }>(),
  {
    isEditable: false,
  }
);
const emit = defineEmits(["edit", "delete", "move", "updatePage"]);
const showMenu = ref<boolean>(false);
const showAddLinkModal = ref<boolean>(false);
const showAddFileModal = ref<boolean>(false);
const currentlySaving = ref<boolean>(false);
const linkModel = ref({
  linkToExternalFile: "",
  alternateFileName: "",
  fileTag: "",
});
const fileModel = ref({ file: undefined, alternateFileName: "", fileTag: "" });
const rerenderKey = ref<string>(crypto.randomUUID());
const linkmetadata: ITableMetaData = {
  label: "Hyperlink",
  id: "Types",
  name: "Types",
  schemaId: props.schema,
  tableType: "DATA",
  columns: [
    {
      id: "linkToExternalFile",
      columnType: "HYPERLINK" as ColumnType,
      label: "External Link",
      required: true,
    },
    {
      id: "alternateFileName",
      columnType: "String" as ColumnType,
      label: "Label",
    },
    {
      id: "fileTag",
      columnType: "String" as ColumnType,
      label: "Tag",
    },
  ],
};
const filemetadata: ITableMetaData = {
  label: "File",
  id: "Types",
  name: "Types",
  schemaId: props.schema,
  tableType: "DATA",
  columns: [
    {
      id: "file",
      columnType: "FILE" as ColumnType,
      label: "File",
      required: true,
    },
    {
      id: "alternateFileName",
      columnType: "String" as ColumnType,
      label: "Label",
    },
    {
      id: "fileTag",
      columnType: "String" as ColumnType,
      label: "Tag",
    },
  ],
};
const fileform = useForm(filemetadata, fileModel);
const linkform = useForm(linkmetadata, linkModel);

async function addNewFile() {
  currentlySaving.value = true;
  await UploadFile(
    props.schema,
    "File-" + crypto.randomUUID(),
    fileModel.value.alternateFileName,
    fileModel.value.fileTag,
    fileModel.value.file
  );
  showAddFileModal.value = false;
  currentlySaving.value = false;
  rerenderKey.value = crypto.randomUUID();
  emit("updatePage");
}
async function addNewLink() {
  currentlySaving.value = true;
  await AddLink(
    props.schema,
    "Link-" + crypto.randomUUID(),
    linkModel.value.linkToExternalFile,
    linkModel.value.alternateFileName,
    linkModel.value.fileTag
  );
  showAddLinkModal.value = false;
  currentlySaving.value = false;
  linkModel.value = {
    linkToExternalFile: "",
    alternateFileName: "",
    fileTag: "",
  };
  rerenderKey.value = crypto.randomUUID();
  emit("updatePage");
}
</script>

<template>
  <div v-if="isEditable">
    <VMenu
      v-model:shown="showMenu"
      showGroup="component-menu"
      :triggers="['hover', 'focus']"
      :popperTriggers="['hover', 'focus']"
      :delay="{ show: 100, hide: 50 }"
      :placement="'bottom-start'"
      noAutoFocus
    >
      <template #popper>
        <div>
          <ComponentActions
            name="File list"
            :id="`${id}-toolbar`"
            :aria-controls="id"
            @edit="$emit('edit')"
            @delete="$emit('delete')"
            @move="$emit('move', $event)"
          />
        </div>
      </template>
      <div>
        <FileDownload v-bind="props" :key="rerenderKey" />
      </div>
    </VMenu>
    <div class="flex gap-2">
      <Button icon="UploadFile" size="tiny" @click="showAddFileModal = true">
        <span>Add a new file</span>
      </Button>
      <Button icon="AddLink" size="tiny" @click="showAddLinkModal = true">
        <span>Add a external link</span>
      </Button>
    </div>

    <Modal
      v-model:visible="showAddLinkModal"
      subtitle="Add a new external link"
      title="add link"
    >
      <div class="p-8">
        <ClientOnly>
          <FormFields id="form-hyperlink" :form="linkform" />
        </ClientOnly>
      </div>

      <template #footer>
        <menu class="flex items-center justify-end h-[116px]">
          <div class="flex gap-4">
            <Button type="secondary" @click="showAddLinkModal = false">
              Cancel
            </Button>
            <Button
              type="primary"
              @click="addNewLink()"
              :disabled="currentlySaving"
            >
              Add link
              <BaseIcon
                v-if="currentlySaving"
                class="inline animate-spin"
                name="ProgressActivity"
              />
            </Button>
          </div>
        </menu>
      </template>
    </Modal>

    <Modal
      v-model:visible="showAddFileModal"
      subtitle="Add a new file"
      title="add file"
    >
      <div class="p-8">
        <ClientOnly>
          <FormFields id="form-file" :form="fileform" />
        </ClientOnly>
      </div>

      <template #footer>
        <menu class="flex items-center justify-end h-[116px]">
          <div class="flex gap-4">
            <Button type="secondary" @click="showAddFileModal = false">
              Cancel
            </Button>
            <Button
              type="primary"
              @click="addNewFile()"
              :disabled="currentlySaving"
            >
              Add file
              <BaseIcon
                v-if="currentlySaving"
                class="inline animate-spin"
                name="ProgressActivity"
              />
            </Button>
          </div>
        </menu>
      </template>
    </Modal>
  </div>

  <FileDownload v-else v-bind="props" />
</template>
