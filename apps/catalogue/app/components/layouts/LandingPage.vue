<script setup lang="ts">
import { useRoute } from "#app";
import { computed } from "vue";
import { useHeaderData } from "../../composables/useHeaderData";
import { useBannerData } from "../../composables/useBannerData";
import HeaderCatalogue from "../header/Catalogue.vue";
import HeaderGlobal from "../header/Global.vue";
import Container from "../../../../tailwind-components/app/components/Container.vue";
import FooterComponent from "../../../../tailwind-components/app/components/FooterComponent.vue";
import FooterVersion from "../footer/FooterVersion.vue";
import Banner from "../../../../tailwind-components/app/components/Banner.vue";

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
    :logoSrc="headerData.logoSrc"
  />
  <HeaderGlobal v-else :logoSrc="headerData.logoSrc" />
  <Container>
    <slot></slot>
  </Container>
  <FooterComponent>
    <ClientOnly>
      <FooterVersion />
    </ClientOnly>
  </FooterComponent>
</template>
