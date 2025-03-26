<script setup lang="ts">
import type { IDocumentation } from "../../types/types";

const { documents } = defineProps<{
  title: string;
  description?: string;
  documents: IDocumentation[];
}>();

function looksLikeImage(document: IDocumentation) {
  return (
    document?.file?.extension &&
    ["jpg", "jpeg", "jfif", "pjpeg", "pjp", "png", "svg", "webp"].includes(
      document.file.extension
    )
  );
}

function isExternalDocument(document: IDocumentation) {
  return document?.file.url ? false : true;
}

const images = documents.filter(looksLikeImage);
const otherDocuments = documents.filter((d) => !looksLikeImage(d));
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <div class="grid gap-2.5">
      <FileList v-if="images?.length" :columnCount="3">
        <FileImageCard
          v-for="image in images"
          :title="image?.name"
          :url="image.url ? image.url : image?.file?.url"
        />
      </FileList>
      <FileList v-if="otherDocuments?.length" :columnCount="2">
        <FileDocumentCard
          v-for="document in otherDocuments"
          :title="document?.name"
          :isExternal="isExternalDocument(document)"
          :url="document?.file?.url ? document?.file?.url : document.url"
        />
      </FileList>
    </div>
  </ContentBlock>
</template>
