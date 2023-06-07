<script setup lang="ts">
const { collectionEvents } = defineProps<{
  title: string;
  description?: string;
  collectionEvents?: ICollectionEvent[];
}>();

const dataCategories = collectionEvents
  ?.flatMap(c => c.dataCategories)
  .filter(e => e !== undefined);
const sampleCategories = collectionEvents
  ?.flatMap(c => c.sampleCategories)
  .filter(e => e !== undefined);
const areasOfInformation = collectionEvents
  ?.flatMap(c => c.areasOfInformation)
  .filter(e => e !== undefined);
</script>

<template>
  <ContentBlock
    :title="title"
    :description="description"
    v-if="
      collectionEvents &&
      (dataCategories?.length ||
        sampleCategories?.length ||
        areasOfInformation?.length)
    ">
    <div class="grid gap-[45px] mt-7.5">
      <ListCollapsible
        v-if="dataCategories?.length"
        title="Data categories"
        :collapse-all="false">
        <ContentOntology
          :tree="buildOntologyTree(dataCategories)"></ContentOntology>
      </ListCollapsible>
      <ListCollapsible
        v-if="sampleCategories?.length"
        title="Sample categories"
        :collapse-all="false">
        <ContentOntology
          :tree="buildOntologyTree(sampleCategories)"></ContentOntology>
      </ListCollapsible>
      <ListCollapsible
        v-if="areasOfInformation?.length"
        title="Areas of information"
        :collapse-all="false">
        <ContentOntology
          :tree="buildOntologyTree(areasOfInformation)"></ContentOntology>
      </ListCollapsible>
    </div>
  </ContentBlock>
</template>
