<script setup lang="ts">
import { ref } from "vue";
import type { IOntologyItem } from "../../../types/types";
import ContentOntology from "../../components/content/Ontology.vue";

const collapseAll = ref(true);

const flatList: IOntologyItem[] = [
  { name: "Item 1" },
  { name: "Item 2" },
  { name: "Item 3" },
];

const flatListWithDefinitions: IOntologyItem[] = [
  { name: "Genomics", definition: "Study of genomes and their functions" },
  { name: "Proteomics", definition: "Large-scale study of proteins" },
  {
    name: "Metabolomics",
    definition: "Study of metabolites within cells and organisms",
  },
];

const singleItem: IOntologyItem[] = [
  {
    name: "Single Item",
    definition: "This is a single item with a definition tooltip",
  },
];

const treeStructure: IOntologyItem[] = [
  {
    name: "Diseases",
    children: [
      {
        name: "Cardiovascular",
        children: [
          { name: "Hypertension", definition: "High blood pressure condition" },
          { name: "Heart failure" },
        ],
      },
      {
        name: "Neurological",
        children: [
          { name: "Alzheimer's disease" },
          {
            name: "Parkinson's disease",
            definition: "Progressive nervous system disorder",
          },
        ],
      },
    ],
  },
  {
    name: "Biomarkers",
    definition: "Measurable indicators of biological state",
    children: [{ name: "Blood markers" }, { name: "Genetic markers" }],
  },
];
</script>

<template>
  <div class="p-4 space-y-8">
    <h1 class="text-2xl font-bold mb-4">ContentOntology Component</h1>

    <div class="mb-4">
      <label class="flex items-center gap-2">
        <input type="checkbox" v-model="collapseAll" />
        <span>Collapse all (for tree structures)</span>
      </label>
    </div>

    <section class="border p-4 rounded">
      <h2 class="text-xl font-semibold mb-2">Single Item</h2>
      <p class="text-gray-600 mb-4">
        Displays as inline text with optional tooltip
      </p>
      <ContentOntology :tree="singleItem" />
    </section>

    <section class="border p-4 rounded">
      <h2 class="text-xl font-semibold mb-2">Flat List</h2>
      <p class="text-gray-600 mb-4">
        Items without children display as bullet list
      </p>
      <ContentOntology :tree="flatList" />
    </section>

    <section class="border p-4 rounded">
      <h2 class="text-xl font-semibold mb-2">Flat List with Definitions</h2>
      <p class="text-gray-600 mb-4">Hover over info icon to see definitions</p>
      <ContentOntology :tree="flatListWithDefinitions" />
    </section>

    <section class="border p-4 rounded">
      <h2 class="text-xl font-semibold mb-2">Tree Structure</h2>
      <p class="text-gray-600 mb-4">
        Nested items with expand/collapse functionality
      </p>
      <ContentOntology :tree="treeStructure" :collapse-all="collapseAll" />
    </section>

    <section class="border p-4 rounded bg-blue-900 text-white">
      <h2 class="text-xl font-semibold mb-2">Inverted (dark background)</h2>
      <p class="text-gray-300 mb-4">Use inverted prop for dark backgrounds</p>
      <ContentOntology :tree="flatListWithDefinitions" :inverted="true" />
    </section>
  </div>
</template>
