<script setup lang="ts">
import Image from "../../components/cms/Image.vue";
import DemoImage from "../../assets/img/molgenis-banner.jpg";
</script>

<template>
  <Image
    id="demo-image"
    :image="{ url: DemoImage }"
    alt="MOLGENIS: open source software"
    class="mt-5"
    width="350px"
  />
</template>
