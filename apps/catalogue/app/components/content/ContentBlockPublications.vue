<script setup lang="ts">
import type { IPublications } from "../../../interfaces/catalogue";
import FileDocumentCard from "../FileDocumentCard.vue";
import FileList from "../FileList.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";

defineProps<{
  title: string;
  description?: string;
  publications: IPublications[];
}>();
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <div class="grid gap-2.5">
      <FileList v-if="publications?.length" :columnCount="2">
        <FileDocumentCard
          v-for="publication in publications"
          :title="publication.title || publication.doi"
          :isExternal="true"
          :url="publication.doi"
        />
      </FileList>
    </div>
  </ContentBlock>
</template>
