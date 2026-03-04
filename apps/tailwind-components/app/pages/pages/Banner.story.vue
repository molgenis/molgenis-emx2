<script setup lang="ts">
import PageBanner from "../../components/pages/Banner.vue";
import DemoImage from "../../assets/img/molgenis-banner.jpg";
import TextHeading from "../../components/text/Heading.vue";
</script>

<template>
  <TextHeading id="h2-banner-default" class="my-5">
    Page Banner without image (default)
  </TextHeading>
  <PageBanner
    id="page-banner-demo"
    title="About Us"
    subtitle="Duis aliquip reprehenderit nulla et voluptate ex eu qui cillum minim."
  />
  <TextHeading id="h2-banner-image" class="my-5">
    Page Banner with image
  </TextHeading>
  <PageBanner
    id="page-banner-demo"
    :background-image="DemoImage"
    title="About Us"
    subtitle="Nostrud aute exercitation ipsum voluptate commodo et laborum ad labore ad elit."
  />
</template>
