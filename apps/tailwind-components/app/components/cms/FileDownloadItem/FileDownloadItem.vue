<script setup lang="ts">
import { computed, ref } from "vue";
import type { IFiles } from "../../../../types/cms";

const props = withDefaults(defineProps<IFiles>(), {});
const emit = defineEmits(["edit", "delete", "move"]);
const showMenu = ref<boolean>(false);

const displayName = computed<string | undefined>(() => {
  if (props.label) {
    return props.label;
  }
  if (props.file?.filename) {
    return props.file.filename;
  }
  if (props.file?.url) {
    return props.file?.url;
  }
  if (props.externalLink) {
    return props.externalLink;
  }
});
</script>

<template>
  <div class="w-full flex">
    <a
      v-if="props.file?.url"
      :href="props.file?.url"
      target="_blank"
      rel="noopener noreferrer"
      class="flex text-title-contrast"
    >
      {{ displayName }}
      <BaseIcon name="UploadFile" :width="16" />
    </a>
    <a
      v-else-if="props.externalLink"
      :href="props.externalLink"
      target="_blank"
      rel="noopener noreferrer"
      class="flex text-title-contrast"
    >
      {{ displayName }}
      <BaseIcon name="ExternalLink" :width="16" />
    </a>
    <span v-else class="ml-auto"> No file uploaded </span>
    <span v-if="props.tag" class="ml-2 text-title-contrast">
      ({{ props.tag }})
    </span>
    <span v-if="props.file?.size" class="ml-2 text-title-contrast">
      ({{ props.file?.size }})
    </span>
    <span v-if="props.file?.extension" class="ml-2 text-title-contrast">
      ({{ props.file?.extension }})
    </span>
  </div>
</template>
