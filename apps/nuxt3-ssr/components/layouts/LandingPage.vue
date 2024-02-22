<script setup lang="ts">
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
  <UIContainer>
    <slot></slot>
  </UIContainer>
  <FooterComponent />
</template>
