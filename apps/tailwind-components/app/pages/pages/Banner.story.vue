<script setup lang="ts">
import PageBanner from "../../components/pages/Banner.vue";
import DemoImage from "../../assets/img/molgenis-banner.jpg";
import Heading from "../../components/text/Heading.vue";
</script>

<template>
  <Heading heading-level="H2" class="my-5"
    >Page Banner without image (default)</Heading
  >
  <PageBanner
    id="page-banner-demo"
    title="About Us"
    subtitle="Duis aliquip reprehenderit nulla et voluptate ex eu qui cillum minim."
  />
  <Heading heading-level="H2" class="my-5">Page Banner with image</Heading>
  <PageBanner
    id="page-banner-demo"
    :background-image="DemoImage"
    title="About Us"
    subtitle="Nostrud aute exercitation ipsum voluptate commodo et laborum ad labore ad elit."
  />
</template>
