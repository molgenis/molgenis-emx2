<script setup lang="ts">
import PageBanner from "../../components/pages/Banner.vue";
import DemoImage from "../../assets/img/molgenis-banner.jpg";
</script>

<template>
  <PageBanner
    id="page-banner-demo"
    :background-image="DemoImage"
    title="My Demo Page"
    subtitle="A description about my custom page"
  />
</template>
