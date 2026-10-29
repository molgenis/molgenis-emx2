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
  files.value = await getFiles(
    props.schema || "",
    props.showFilesWithTag || ""
  );
};

watch([() => props.showFilesWithTag, () => props.schema], loadFiles, {
  immediate: true,
});
</script>

<template>
  <div class="w-full py-8 justify-center items-center">
    <div class="m-auto w-pg-section">
      <p :id="`${id}-list-title`" class="sr-only">
        files available for download
      </p>
      <ul :aria-labelledBy="`${id}-list-title`">
        <li v-for="file in files">
          <FileDownloadItem
            :key="file.id || file.alternateFileName"
            v-bind="file"
          />
        </li>
      </ul>
    </div>
  </div>
</template>
