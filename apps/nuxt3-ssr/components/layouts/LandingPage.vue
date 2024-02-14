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
    <slot></slot>
  </Container>
  <FooterComponent />
</template>
