<script setup lang="ts">
const { collectionEvents } = defineProps<{
  title: string;
  description?: string;
  collectionEvents?: ICollectionEvent[];
}>();
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <div v-if="collectionEvents" class="grid gap-[45px] mt-7.5">
      <CategoriesTree
        title="Data categories"
        category="dataCategories"
        :collectionEvents="collectionEvents"
      />
      <CategoriesTree
        title="Sample categories"
        :columnCount="3"
        category="sampleCategories"
        :collectionEvents="collectionEvents"
      />
      <CategoriesTree
        title="Areas of informations"
        category="areasOfInformation"
        :collectionEvents="collectionEvents"
      />
    </div>
  </ContentBlock>
</template>
