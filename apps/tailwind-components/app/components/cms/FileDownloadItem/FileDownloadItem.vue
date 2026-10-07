<script setup lang="ts">
import { computed, ref } from "vue";
import type { IFiles } from "../../../../types/cms";
import { readableFileSize } from "../../../utils/readableFileSize";

const props = withDefaults(defineProps<IFiles>(), {});
const emit = defineEmits(["edit", "delete", "move"]);
const showMenu = ref<boolean>(false);

const displayName = computed<string | undefined>(() => {
  if (props.alternateFileName) {
    return props.alternateFileName;
  }
  if (props.linkToExternalFile) {
    return props.linkToExternalFile;
  }
  if (props.file?.filename) {
    return props.file.filename.split(".").slice(0, -1).join(".");
  }
  if (props.file?.url) {
    return props.file?.url;
  }
});

const link = computed<string | undefined>(() => {
  if (props.file?.url) {
    return props.file?.url;
  } else if (props.linkToExternalFile) {
    return props.linkToExternalFile;
  }
});

const linkIcon = computed<string | undefined>(() => {
  if (
    props.file?.extension === "png" ||
    props.file?.extension === "jpg" ||
    props.file?.extension === "jpeg" ||
    props.file?.extension === "gif"
  ) {
    return "Image";
  } else if (props.file?.url) {
    return "UploadFile";
  } else {
    return "ExternalLink";
  }
});
</script>

<template>
  <div
    class="w-full border rounded-base mb-2.5 grid grid-cols-[1fr_50px] justify-stretch text-title-contrast"
  >
    <div
      class="w-full p-2.5 gap-2 grid grid-cols-[1fr_minmax(0,10rem)_minmax(0,10rem)_50px] whitespace-nowrap"
    >
      <span class="pl-3.5 py-2.5 flex">
        <BaseIcon class="mr-2" :name="linkIcon || ''" :width="16" />
        {{ displayName }}
        <span
          v-if="props.fileTag"
          class="text-body-xs text- bg-button-primary text-button-primary rounded-full px-2.5 py-1 ml-2"
        >
          {{ props.fileTag }}
        </span>
      </span>
      <span class="py-2.5">
        <span
          v-if="props.file?.extension"
          class="text-body-sm text-title-contrast"
        >
          {{ props.file?.extension }}
        </span>
      </span>
      <span class="py-2.5">
        <span v-if="props.file?.size" class="text-body-sm text-title-contrast">
          {{ readableFileSize(props.file?.size) }}
        </span>
      </span>
    </div>
    <div>
      <a
        :href="link"
        target="_blank"
        rel="noopener noreferrer"
        class="h-full flex rounded-base justify-content px-3.5 rounded-l-none bg-button-primary text-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
        :download="props.file?.url ? true : false"
      >
        <span v-if="props.fileIsAnExternalLink" class="hidden">
          Link to {{ linkToExternalFile }}</span
        >
        <span v-else class="hidden"> Download {{ file?.filename }}</span>
        <BaseIcon
          :name="linkToExternalFile ? 'ExternalLink' : 'Download'"
          :width="20"
          class="text"
        />
      </a>
    </div>
  </div>
</template>
