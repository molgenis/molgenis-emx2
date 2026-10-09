<script setup lang="ts">
import { useTemplateRef, onMounted, onUpdated } from "vue";
import type {
  ICustomComponents,
  IDeveloperPages,
} from "../../../../types/cms.ts";
import { generateHtmlPreview } from "../../../utils/cms";

const previewElem = useTemplateRef<HTMLDivElement>("preview");
const props = defineProps<ICustomComponents>();

function renderPreview() {
  const content: IDeveloperPages = {
    name: "",
    description: "",
    html: props.html,
    css: props.css,
    javascript: props.js,
  };
  generateHtmlPreview(content, previewElem.value as HTMLDivElement);
  console.log("renderPreview", props.html, previewElem.value);
}

onMounted(() => {
  renderPreview();
});
onUpdated(() => {
  renderPreview();
});
</script>

<template>
  <div v-if="html === ''">No content available</div>
  <div v-else ref="preview" />
</template>
