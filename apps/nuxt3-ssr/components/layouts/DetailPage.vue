<script setup lang="ts">
import Container from "./../Container.vue";
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
    <div class="xl:flex xl:items-start">
      <aside class="xl:w-82.5 sticky top-[30px] flex-shrink-0 hidden xl:block">
        <slot name="side"></slot>
      </aside>

      <div class="xl:pl-7.5 grow min-w-0">
        <slot name="main"></slot>
      </div>
    </div>
  </Container>
  <FooterComponent />
</template>
