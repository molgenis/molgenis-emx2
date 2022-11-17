<script setup>
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
      <FileList title="Images" columnCount="3">
        <FileImageCard
          title="Vragenlijst en huisbezoeken infographic.png"
          url="https://via.placeholder.com/174x96"
        />
        <FileImageCard
          title="Random afbeelding.jpg"
          url="https://via.placeholder.com/1500x1100"
        />
        <FileImageCard
          title="UMCG logo.png"
          url="https://via.placeholder.com/150x600"
        />
      </FileList>
      <FileList title="Documents" columnCount="2">
        <FileDocumentCard
          title="The public documentation of Lifelines NEXT Cohort.pdf"
          url="#"
        />
        <FileDocumentCard title="Variables.xls" url="#" />
      </FileList>
    </div>
  </ContentBlock>
</template>
