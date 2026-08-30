<script setup lang="ts">
import { ref } from "vue";
import DisplayOntology from "../../components/display/Ontology.vue";
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

const whiteBackground = ref(true);
</script>

<template>
  <Story
    title="DisplayOntology"
    description="Displays ontology values as single item, flat list, or collapsible tree depending on data structure."
  >
    <div class="p-5 space-y-6">
      <div>
        <input
          id="ontology-story-white-background"
          class="hover:cursor-pointer"
          type="checkbox"
          v-model="whiteBackground"
        />
        <label
          class="ml-1 hover:cursor-pointer"
          for="ontology-story-white-background"
        >
          white background
        </label>
        <p class="text-sm text-record-label">
          Mirrors what a real caller passes: the single item and flat list take
          `inverted` from this switch and dim their tooltip hover accordingly,
          the hierarchical tree's tooltip stays white regardless.
        </p>
      </div>

      <div
        class="p-6 rounded shadow-primary space-y-6 text-record-value"
        :class="whiteBackground ? 'bg-content' : ''"
      >
        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">Single Item</h2>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
            <span class="font-medium text-record-label">No definition:</span>
            <DisplayOntology :value="singleItem" :inverted="!whiteBackground" />
          </div>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
            <span class="font-medium text-record-label">With definition:</span>
            <DisplayOntology
              :value="singleWithDefinition"
              :inverted="!whiteBackground"
            />
          </div>
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">Flat List</h2>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
            <span class="font-medium text-record-label">No definitions:</span>
            <DisplayOntology :value="flatList" :inverted="!whiteBackground" />
          </div>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
            <span class="font-medium text-record-label">With definitions:</span>
            <DisplayOntology
              :value="flatListWithDefinitions"
              :inverted="!whiteBackground"
            />
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
            <span class="font-medium text-record-label">Tree (collapsed):</span>
            <DisplayOntology
              :value="hierarchicalTree"
              :collapse-all="true"
              :inverted="!whiteBackground"
            />
          </div>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
            <span class="font-medium text-record-label"
              >Tree (root expanded):</span
            >
            <DisplayOntology
              :value="hierarchicalTree"
              :collapse-all="false"
              :inverted="!whiteBackground"
            />
          </div>
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">Empty Value</h2>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
            <span class="font-medium text-record-label">Empty array:</span>
            <DisplayOntology :value="[]" :inverted="!whiteBackground" />
          </div>
        </div>
      </div>
    </div>
  </Story>
</template>
