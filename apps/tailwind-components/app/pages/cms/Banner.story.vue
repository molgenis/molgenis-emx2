<script setup lang="ts">
import PageBanner from "../../components/cms/Banner.vue";
import DemoImage from "../../assets/img/molgenis-banner.jpg";
import TextHeading from "../../components/cms/Heading.vue";
</script>

<template>
  <TextHeading
    id="h2-banner-default"
    class="my-5"
    :level="3"
    text="Page Banner without image (default)"
  />
  <PageBanner
    id="page-banner-demo"
    title="About us"
    subtitle="Enim Lorem ut velit aute sunt."
  />
  <TextHeading
    id="h2-banner-image"
    class="my-5"
    :level="3"
    text="Page Banner with image"
  />
  <PageBanner
    id="page-banner-demo"
    :image="{ url: DemoImage }"
    title="About us"
    subtitle="Mollit cupidatat voluptate non occaecat est nulla."
  />
</template>
