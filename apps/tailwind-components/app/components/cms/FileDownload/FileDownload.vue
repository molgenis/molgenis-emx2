<script setup lang="ts">
import { ref, watch } from "vue";
import type { IFileLists, IFiles } from "../../../../types/cms";
import { getFiles } from "../../../utils/cms";
import FileDownloadItem from "../FileDownloadItem/FileDownloadItem.vue";
const props = withDefaults(
  defineProps<IFileLists & { isEditable?: boolean; schema: string }>(),
  {
    isEditable: false,
  }
);
let files = ref<IFiles[]>([]);

const loadFiles = async () => {
  files.value = await getFiles(props.schema || "", props.shownTag || "");
};

watch([() => props.shownTag, () => props.schema], loadFiles, {
  immediate: true,
});
</script>

<template>
  <div class="w-full py-8 justify-center items-center">
    <div class="m-auto w-pg-section">
      <FileDownloadItem
        v-for="file in files"
        :key="file.id || file.label"
        v-bind="file"
      />
    </div>
  </div>
</template>
