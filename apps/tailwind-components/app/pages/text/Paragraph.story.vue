<script setup lang="ts">
import TextParagraph from "../../components/text/Paragraph.vue";
</script>

<template>
  <div class="text-title-contrast w-3/5">
    <TextParagraph class="my-5">
      <strong>This is a normal paragraph</strong>. In voluptate ullamco est
      irure fugiat tempor laborum Lorem mollit reprehenderit. Lorem aute minim
      dolor excepteur labore do nisi ex.
    </TextParagraph>
    <TextParagraph :is-centered="true">
      <strong>This a centered paragraph</strong>. Ex culpa minim irure sunt ut
      dolor laborum tempor consequat cupidatat laboris. Duis veniam esse
      deserunt dolor exercitation culpa reprehenderit. Exercitation eiusmod est
      aute Lorem nostrud ipsum.
    </TextParagraph>
  </div>
</template>
