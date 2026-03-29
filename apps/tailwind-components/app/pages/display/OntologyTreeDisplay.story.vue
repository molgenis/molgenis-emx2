<script setup lang="ts">
import OntologyTreeDisplay from "../../components/display/OntologyTreeDisplay.vue";
import type { IOntologyTreeItem } from "../../utils/buildOntologyTree";

const singleItem: IOntologyTreeItem = { name: "Biobank" };

const singleWithDefinition: IOntologyTreeItem = {
  name: "Biobank",
  definition: "A collection of biological samples and associated data.",
};

const flatList: IOntologyTreeItem[] = [
  { name: "Genomics" },
  { name: "Proteomics" },
  { name: "Metabolomics" },
];

const flatListWithDefinitions: IOntologyTreeItem[] = [
  { name: "Genomics", definition: "Study of genomes" },
  { name: "Proteomics", definition: "Study of proteins" },
  { name: "Metabolomics", definition: "Study of metabolites" },
];

const hierarchicalTree: IOntologyTreeItem[] = [
  {
    name: "Cardiology",
    definition: "Study of heart",
    parent: { name: "Medicine", definition: "Medical science" },
  },
  {
    name: "Neurology",
    definition: "Study of nervous system",
    parent: { name: "Medicine", definition: "Medical science" },
  },
  {
    name: "Pediatric Cardiology",
    parent: {
      name: "Cardiology",
      definition: "Study of heart",
      parent: { name: "Medicine", definition: "Medical science" },
    },
  },
];
</script>

<template>
  <Story
    title="OntologyTreeDisplay"
    description="Displays ontology values as single item, flat list, or collapsible tree depending on data structure."
  >
    <div class="p-5 space-y-6">
      <div
        class="bg-content p-6 rounded shadow-primary space-y-6 text-record-value"
      >
        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">Single Item</h2>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
            <span class="font-medium text-record-label">No definition:</span>
            <OntologyTreeDisplay :value="singleItem" />
          </div>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
            <span class="font-medium text-record-label">With definition:</span>
            <OntologyTreeDisplay :value="singleWithDefinition" />
          </div>
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">Flat List</h2>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
            <span class="font-medium text-record-label">No definitions:</span>
            <OntologyTreeDisplay :value="flatList" />
          </div>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
            <span class="font-medium text-record-label">With definitions:</span>
            <OntologyTreeDisplay :value="flatListWithDefinitions" />
          </div>
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">
            Hierarchical Tree
          </h2>
          <p class="text-sm text-gray-500">
            Items linked via parent chain are grouped into a collapsible tree.
            Click the caret to expand/collapse.
          </p>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
            <span class="font-medium text-record-label">Tree (collapsed):</span>
            <OntologyTreeDisplay
              :value="hierarchicalTree"
              :collapse-all="true"
            />
          </div>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
            <span class="font-medium text-record-label">Tree (expanded):</span>
            <OntologyTreeDisplay
              :value="hierarchicalTree"
              :collapse-all="false"
            />
          </div>
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">Empty Value</h2>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
            <span class="font-medium text-record-label">Empty array:</span>
            <OntologyTreeDisplay :value="[]" />
          </div>
        </div>
      </div>
    </div>
  </Story>
</template>
