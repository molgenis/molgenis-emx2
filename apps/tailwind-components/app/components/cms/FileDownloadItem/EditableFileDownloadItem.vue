<script setup lang="ts">
import { ref } from "vue";
import type { IFiles } from "../../../../types/cms";
import ComponentActions from "../ComponentActions.vue";
import FileDownloadItem from "./FileDownloadItem.vue";
import UploadNeeded from "../UploadNeeded.vue";

const props = withDefaults(defineProps<IFiles & { isEditable?: boolean }>(), {
  isEditable: false,
});
const emit = defineEmits(["edit", "delete", "move"]);
const showMenu = ref<boolean>(false);
</script>

<template>
  <VMenu
    v-if="isEditable"
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
        name="File"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        @move="$emit('move', $event)"
      />
    </template>
    <div>
      <UploadNeeded v-if="!props.file?.url && !props.linkToExternalFile"
        >Click the edit button to upload an file or set an external
        link</UploadNeeded
      >
      <FileDownloadItem v-else v-bind="props" />
    </div>
  </VMenu>
  <FileDownloadItem v-else v-bind="props" />
</template>
