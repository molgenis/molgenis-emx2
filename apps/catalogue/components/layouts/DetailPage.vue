<script setup lang="ts">
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
