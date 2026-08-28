<script setup lang="ts">
import type { ICollectionEvents } from "../../../interfaces/catalogue";
import ListCollapsible from "../ListCollapsible.vue";
import OntologyTreeDisplay from "../../../../tailwind-components/app/components/display/OntologyTreeDisplay.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";

const { collectionEvents } = defineProps<{
  title: string;
  description?: string;
  collectionEvents?: ICollectionEvents[];
}>();

const dataCategories = collectionEvents
  ?.flatMap((c) => c.dataCategories)
  .filter((e) => e !== undefined);
const sampleCategories = collectionEvents
  ?.flatMap((c) => c.sampleCategories)
  .filter((e) => e !== undefined);
const areasOfInformation = collectionEvents
  ?.flatMap((c) => c.areasOfInformation)
  .filter((e) => e !== undefined);
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
    "
  >
    <div class="grid gap-[45px] mt-7.5">
      <ListCollapsible
        v-if="dataCategories?.length"
        title="Data categories"
        :collapse-all="false"
      >
        <OntologyTreeDisplay
          :value="dataCategories"
          :inverted="true"
        ></OntologyTreeDisplay>
      </ListCollapsible>
      <ListCollapsible
        v-if="sampleCategories?.length"
        title="Sample categories"
        :collapse-all="false"
      >
        <OntologyTreeDisplay
          :value="sampleCategories"
          :inverted="true"
        ></OntologyTreeDisplay>
      </ListCollapsible>
      <ListCollapsible
        v-if="areasOfInformation?.length"
        title="Areas of information"
        :collapse-all="false"
      >
        <OntologyTreeDisplay
          :value="areasOfInformation"
          :inverted="true"
        ></OntologyTreeDisplay>
      </ListCollapsible>
    </div>
  </ContentBlock>
</template>
