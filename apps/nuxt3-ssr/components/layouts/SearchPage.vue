<script setup>
const route = useRoute();
const headerData = await useHeaderData();
const catalogue = headerData.catalogue;
const variableCount = headerData.variableCount;
</script>

<template>
  <HeaderCatalogue
    v-if="route.params.catalogue"
    :catalogue="catalogue"
    :variableCount="variableCount"
  />
  <HeaderGlobal v-else />
  <Container>
    <slot name="header"></slot>
    <div class="xl:flex">
      <aside class="xl:min-w-95 xl:w-95 hidden xl:block">
        <slot name="side"></slot>
      </aside>
      <div class="xl:pl-7.5 xl:grow">
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
