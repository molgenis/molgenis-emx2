<script setup lang="ts">
import { ref } from "vue";
import type { IFileLists } from "../../../../types/cms";
import { getFiles } from "../../../utils/cms";
import { useRoute } from "vue-router";
import FileDownloadItem from "../FileDownloadItem/FileDownloadItem.vue";
const props = withDefaults(
  defineProps<IFileLists & { isEditable?: boolean; schema: string }>(),
  {
    isEditable: false,
  }
);
const route = useRoute();

const files = await getFiles(props.schema || "", props.shownTag || "");
</script>

<template>
  <div class="w-full py-8 justify-center items-center">
    <div class="m-auto w-pg-section">
      <FileDownloadItem v-for="file in files" :key="file.label" v-bind="file" />
    </div>
  </div>
</template>
