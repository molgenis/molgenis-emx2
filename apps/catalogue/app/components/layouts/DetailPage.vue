<script setup lang="ts">
import { useRoute } from "#app";
import { useHeaderData, useBannerData } from "#imports";
import { computed } from "vue";
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
    <slot name="header"></slot>
    <div class="xl:flex xl:items-start">
      <aside
        v-if="$slots.side"
        class="xl:w-82.5 sticky top-[30px] flex-shrink-0 hidden xl:block"
      >
        <slot name="side"></slot>
      </aside>

      <div class="xl:pl-7.5 grow min-w-0">
        <slot name="main"></slot>
      </div>
    </div>
  </Container>
  <FooterComponent>
    <ClientOnly>
      <FooterVersion />
    </ClientOnly>
  </FooterComponent>
</template>
