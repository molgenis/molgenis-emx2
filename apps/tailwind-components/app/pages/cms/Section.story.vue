<script setup lang="ts">
import Section from "../../components/cms/Section.vue";
import Heading from "../../components/cms/Heading.vue";
import Paragraph from "../../components/cms/Paragraph.vue";
</script>

<template>
  <Section id="section-default">
    <Heading id="section-default-heading" :level="3" text="Default section" />
    <Paragraph
      id="section-default-paragraph"
      text="This is the default status of the section component. Content has a fixed width and left aligned."
    />
  </Section>
  <Section id="section-full-width" :enableFullScreenWidth="true">
    <Heading
      id="section-full-width-heading"
      :level="3"
      text="Full width section"
    />
    <Paragraph
      id="section-full-width-paragraph"
      text="Sections can also fit the entire width if the page using the 'enable full screen width' setting. This may be useful if you need more space for content (e.g., images, visualisations, etc.)."
    />
  </Section>
  <Section id="section-visually-emphasized" :applyShadedBackground="true">
    <Heading
      id="section-visually-emphasized-heading"
      :level="3"
      text="Section with visual emphasis"
    />
    <Paragraph
      id="section-visually-emphasized-paragraph"
      text="The 'apply shaded background' setting can be used to add visual distinction between sections. This may be useful if you have pages with a lot of content."
    />
  </Section>
</template>
