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

const inverted = ref(false);
</script>

<template>
  <Story
    title="DisplayOntology"
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
            <DisplayOntology :value="singleItem" />
          </div>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
            <span class="font-medium text-record-label">With definition:</span>
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
            <span class="font-medium text-record-label">With definitions:</span>
            <DisplayOntology :value="flatListWithDefinitions" />
          </div>
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">Inverted</h2>
          <p class="text-sm text-record-label">
            Dims the read-more tooltip's hover colour for use on a dark surface.
            Only the single-item and flat-list branches below pick it up; the
            hierarchical tree's tooltip stays white regardless.
          </p>

          <fieldset class="border border-gray-900 mb-2">
            <legend class="m-2 px-2">Props</legend>
            <div class="mb-2">
              <input
                id="display-ontology-inverted"
                class="ml-2 hover:cursor-pointer"
                type="checkbox"
                v-model="inverted"
              />
              <label
                class="ml-1 hover:cursor-pointer"
                for="display-ontology-inverted"
              >
                inverted
              </label>
            </div>
          </fieldset>

          <div
            class="p-4 rounded space-y-4"
            :class="inverted ? 'bg-white' : 'bg-sidebar-gradient'"
          >
            <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
              <span class="font-medium text-record-label">Single item:</span>
              <DisplayOntology
                :value="singleWithDefinition"
                :inverted="inverted"
              />
            </div>

            <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
              <span class="font-medium text-record-label">Flat list:</span>
              <DisplayOntology
                :value="flatListWithDefinitions"
                :inverted="inverted"
              />
            </div>
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
            <DisplayOntology :value="hierarchicalTree" :collapse-all="true" />
          </div>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
            <span class="font-medium text-record-label"
              >Tree (root expanded):</span
            >
            <DisplayOntology :value="hierarchicalTree" :collapse-all="false" />
          </div>
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">Empty Value</h2>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
            <span class="font-medium text-record-label">Empty array:</span>
            <DisplayOntology :value="[]" />
          </div>
        </div>
      </div>
    </div>
  </Story>
</template>
