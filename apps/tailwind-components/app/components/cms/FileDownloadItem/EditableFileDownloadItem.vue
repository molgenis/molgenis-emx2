<script setup lang="ts">
import { ref } from "vue";
import type { IFiles } from "../../../../types/cms";
import ComponentActions from "../ComponentActions.vue";
import FileDownloadItem from "./FileDownloadItem.vue";

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
    <div
      v-if="!props.file?.url && !props.externalLink"
      class="w-full flex items-center justify-center text-center gap-2 text-title-contrast py-5 border border-button-tertiary rounded-base mb-2.5 hover:border-button-tertiary-hover"
    >
      <BaseIcon name="UploadFile" :width="21" />
      <span
        >Click the edit button to upload an file or set an external link</span
      >
    </div>
    <FileDownloadItem v-else v-bind="props" />
  </VMenu>
  <FileDownloadItem v-else v-bind="props" />
</template>
