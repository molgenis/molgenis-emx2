<script setup lang="ts">
import TextParagraph from "../../components/cms/Paragraph.vue";
</script>

<template>
  <div class="grid grid-cols-1 mt-2.5 text-title-contrast w-3/5">
    <TextParagraph
      id="paragraph-default"
      text="<strong>This is a normal paragraph</strong>. By default, text is rendered as is. Very minor styling is applied, but this is only to assist with spacing when other elements are present."
    />
    <TextParagraph
      id="paragraph-centered"
      :paragraphIsCentered="true"
      text="<strong>This a centered paragraph</strong>. Using the prop <code>paragraphIsCentered='true'</code>, you can align the text to the center of the page."
    />
    <TextParagraph
      id="paragraph-with-html"
      text="<strong>This a paragraph with html</strong>. You can write html within a paragraph, but only certain tags are allowed (e.g., span, a, strong, code, etc.). It is important to remember that all content is inserted into a single paragraph element, which is designed to render text. If you need other elements (images, tables, etc.), then use one of the other components. Links are automatically styled to match the MOLGENIS theme. For example, <a href='mailto:support@molgenis.org'>support@molgenis.org</a>."
    />
  </div>
</template>
