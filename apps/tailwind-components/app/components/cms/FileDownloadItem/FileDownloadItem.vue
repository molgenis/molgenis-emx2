<script setup lang="ts">
import { computed, ref } from "vue";
import type { IFiles } from "../../../../types/cms";

const props = withDefaults(defineProps<IFiles>(), {});
const emit = defineEmits(["edit", "delete", "move"]);
const showMenu = ref<boolean>(false);

const displayName = computed<string | undefined>(() => {
  if (props.file?.filename) {
    return props.file.filename;
  }
  if (props.file?.url) {
    return props.file?.url;
  }
  if (props.externalLink) {
    return props.externalLink;
  }
  if (props.label) {
    return props.label;
  }
});

const link = computed<string | undefined>(() => {
  if (props.useExternalLink) {
    return props.file?.url;
  } else {
    return props.externalLink;
  }
});

const linkIcon = computed<string | undefined>(() => {
  if(props.file?.extension === "png" || props.file?.extension === "jpg" || props.file?.extension === "jpeg" || props.file?.extension === "gif"){
    return "Image";
  }
  if (props.file?.url) {
    return "UploadFile";
  }
  if (props.externalLink) {
    return "ExternalLink";
  }
});

const fileSize = function (size: number) {
  if (isNaN(size)) return "Unknown";
  if (size < 1024) return size + " Bytes";
  if (size < 1024 * 1024) return (size / 1024).toFixed(0) + " KB";
  if (size < 1024 * 1024 * 1024)
    return (size / (1024 * 1024)).toFixed(0) + " MB";
  if (size < 1024 * 1024 * 1024 * 1024)
    return (size / (1024 * 1024 * 1024)).toFixed(2) + " GB";
  return (size / (1024 * 1024 * 1024 * 1024)).toFixed(2) + " TB";
};
</script>

<template>
  <a
    :href="link"
    target="_blank"
    rel="noopener noreferrer"
    class="w-full flex border rounded-base hover:border-button-tertiary-hover mb-2.5 justify-between gap-2"
  >
    <span class="flex items-center gap-1 p-2.5 text-title-contrast">
      <BaseIcon :name="linkIcon || ''" :width="16" />
      {{ displayName }}
      <span v-if="props.tag" class="text-body-xs text-white bg-button-primary rounded-full px-2.5 py-1 ml-2">
        {{ props.tag }}
      </span>
    </span>
    <span v-if="props.file?.extension" class="text-title-contrast p-2.5">
      {{ props.file?.extension }}
    </span>
    <span v-if="props.file?.size" class="text-title-contrast p-2.5">
      {{ fileSize(props.file?.size) }}
    </span>
    <span
      class="flex rounded-base px-3.5 rounded-l-none bg-button-primary text-white"
    >
      <BaseIcon
        :name="externalLink ? 'ExternalLink' : 'Download'"
        :width="20"
        class="text"
      />
    </span>
  </a>
</template>
