<script setup lang="ts">
import DisplayOntology from "../../components/display/Ontology.vue";
import type { IOntologyTreeItem } from "../../../types/types";

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

const mixedRootTree: IOntologyTreeItem[] = [
  { name: "Standalone finding" },
  {
    name: "Cardiology",
    parent: { name: "Medicine" },
  },
  {
    name: "Pediatric Cardiology",
    parent: { name: "Cardiology", parent: { name: "Medicine" } },
  },
];

// Both surfaces render together, always, so one screenshot shows both: an
// interaction (a checkbox toggle) would otherwise sit in the verification
// loop for every theme.
const surfaces = [
  {
    key: "content",
    label: "On a content surface",
    wrapperClass: "bg-content text-title-contrast",
  },
  {
    key: "inverted",
    label: "Off content surface, with surface-inverted",
    wrapperClass: "text-title surface-inverted",
  },
];
</script>

<template>
  <Story
    title="DisplayOntology"
    description="Displays ontology values as single item, flat list, or collapsible tree depending on data structure."
  >
    <div class="p-5 space-y-10">
      <div v-for="surface in surfaces" :key="surface.key" class="space-y-4">
        <h1 class="text-lg font-bold">{{ surface.label }}</h1>

        <div
          class="p-6 rounded shadow-primary space-y-6"
          :class="surface.wrapperClass"
        >
          <div class="space-y-4">
            <h2 class="text-xl font-semibold text-record-heading">
              Single Item
            </h2>

            <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
              <span class="font-medium text-record-label">No definition:</span>
              <DisplayOntology :value="singleItem" />
            </div>

            <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
              <span class="font-medium text-record-label"
                >With definition:</span
              >
              <DisplayOntology :value="singleWithDefinition" />
            </div>
          </div>

          <div class="space-y-4">
            <h2 class="text-xl font-semibold text-record-heading">Flat List</h2>

            <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
              <span class="font-medium text-record-label">No definitions:</span>
              <DisplayOntology :value="flatList" />
            </div>

            <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
              <span class="font-medium text-record-label"
                >With definitions:</span
              >
              <DisplayOntology :value="flatListWithDefinitions" />
            </div>
          </div>

          <div class="space-y-4">
            <h2 class="text-xl font-semibold text-record-heading">
              Hierarchical Tree
            </h2>
            <p class="text-sm text-record-label">
              Items linked via parent chain are grouped into a collapsible tree.
              `collapse-all` only sets the root's own state; deeper levels start
              collapsed regardless. Click a caret to expand/collapse.
            </p>

            <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
              <span class="font-medium text-record-label"
                >Tree (collapsed):</span
              >
              <DisplayOntology :value="hierarchicalTree" :collapse-all="true" />
            </div>

            <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
              <span class="font-medium text-record-label"
                >Tree (root expanded):</span
              >
              <DisplayOntology
                :value="hierarchicalTree"
                :collapse-all="false"
              />
            </div>
          </div>

          <div class="space-y-4">
            <h2 class="text-xl font-semibold text-record-heading">
              Mixed roots (measurement fixture)
            </h2>
            <p class="text-sm text-record-label">
              A root leaf (no caret) next to a root branch, expanded two levels:
              exercises all four row markers together for row-height and
              row-spacing measurement.
            </p>

            <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
              <span class="font-medium text-record-label">Mixed:</span>
              <div :id="`measure-tree-${surface.key}`">
                <DisplayOntology :value="mixedRootTree" :collapse-all="false" />
              </div>
            </div>
          </div>

          <div class="space-y-4">
            <h2 class="text-xl font-semibold text-record-heading">
              Empty Value
            </h2>

            <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
              <span class="font-medium text-record-label">Empty array:</span>
              <DisplayOntology :value="[]" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </Story>
</template>
