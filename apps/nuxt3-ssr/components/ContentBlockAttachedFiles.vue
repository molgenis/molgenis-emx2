<script setup>
import ContentBlock from "./ContentBlock.vue";
import FileList from "./FileList.vue";
import FileImageCard from "./FileImageCard.vue";
import FileDocumentCard from "./FileDocumentCard.vue";

defineProps({
  title: {
    type: String,
  },
  description: {
    type: String,
  },
});
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <div class="grid gap-2.5">
      <FileList title="Images">
        <FileImageCard />
        <FileImageCard />
      </FileList>
      <FileList title="Documents">
        <FileDocumentCard />
        <FileDocumentCard />
      </FileList>
    </div>
  </ContentBlock>
</template>
