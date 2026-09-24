<script setup lang="ts">
import PageHeader from "../../components/cms/header/Header.vue";
import Heading from "../../components/cms/heading/Heading.vue";
import DemoImage from "../../assets/img/molgenis-banner.jpg";
</script>

<template>
  <Heading
    id="h2-banner-default"
    class="my-5"
    :level="3"
    text="Page Banner without image (default)"
  />
  <PageHeader
    id="page-banner-demo"
    title="About us"
    subtitle="Enim Lorem ut velit aute sunt."
  />
  <Heading
    id="h2-banner-image"
    class="my-5"
    :level="3"
    text="Page Banner with image"
  />
  <PageHeader
    id="page-banner-demo"
    :image="{ url: DemoImage }"
    title="About us"
    subtitle="Mollit cupidatat voluptate non occaecat est nulla."
  />
</template>
