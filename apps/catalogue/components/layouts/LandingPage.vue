<script setup lang="ts">
import { useRoute } from "#app";
import { computed } from "vue";
import { useHeaderData } from "../../composables/useHeaderData";
import { useBannerData } from "../../composables/useBannerData";

const route = useRoute();
const headerData = await useHeaderData();
const bannerData = await useBannerData();
const bannerHtml = computed(() => {
  return bannerData.data;
});
</script>

<template>
  <Banner v-if="bannerHtml.value" v-html="bannerHtml.value"> </Banner>
  <HeaderCatalogue
    v-if="route.params.catalogue"
    :catalogue="headerData.catalogue"
    :variableCount="headerData.variableCount"
    :collectionCount="headerData.collectionCount"
    :networkCount="headerData.networkCount"
  />
  <HeaderGlobal v-else />
  <Container>
    <slot></slot>
  </Container>
  <FooterLandingPage />
</template>
