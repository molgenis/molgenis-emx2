<template>
  <div>
    <PageHeader icon="handshake" title="My Title" description='This is some dummy text to create a string of over 250 characters long to show that it is initially truncated. You can view the full text by pressing "..." at the end of this description to view the text that is initially hidden but then suddenly appears and allows the user to read the full description.'>
      <template #title-prefix>
        <ButtonPageHeader label="title-prefix slot with ButtonPageHeader" icon="arrow-left" />
      </template>
      <template #title-suffix>
        <span>title-postfix slot with text</span>
      </template>
    </PageHeader>

    <PageHeader align="left" title="My Title" description="My description">
      <template #prefix>
        <span>Prefix slot</span>
      </template>
      <template #suffix>
        <span>Suffix slot</span>
      </template>
    </PageHeader>
  </div>
</template>
<script setup lang="ts">
import ButtonPageHeader from "../components/button/PageHeader.vue";
</script>