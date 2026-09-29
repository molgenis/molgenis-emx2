<script setup lang="ts">
import { ref } from "vue";
import type { IFileLists } from "../../../../types/cms";
import { AddFile } from "../../../utils/cms";
import ComponentActions from "../ComponentActions.vue";
import FileDownload from "./FileDownload.vue";
import Button from "../../Button.vue";

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

function doSave() {
  currentlySaving.value = true;
  setTimeout(() => {
    currentlySaving.value = false;
    showAddFileModal.value = false;
    showAddLinkModal.value = false;
  }, 1000);
}

async function addNewFile() {
  await AddFile(props.schema, "FileList File - " + crypto.randomUUID());
  showAddFileModal.value = true;
}
async function addNewLink() {
  await AddFile(props.schema, "FileList Link - " + crypto.randomUUID());
  showAddLinkModal.value = true;
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
        <ComponentActions
          name="File list"
          :id="`${id}-toolbar`"
          :aria-controls="id"
          @edit="$emit('edit')"
          @delete="$emit('delete')"
          @move="$emit('move', $event)"
        />
      </template>

      <FileDownload v-bind="props" />
    </VMenu>
    <div class="flex gap-2">
      <Button icon="UploadFile" size="tiny" @click="addNewFile">
        <span>Add a new file</span>
      </Button>
      <Button icon="AddLink" size="tiny" @click="addNewLink">
        <span>Add a external link</span>
      </Button>
    </div>
  </div>

  <FileDownload v-else v-bind="props" />
</template>
