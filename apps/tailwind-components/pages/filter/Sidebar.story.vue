<script setup lang="ts">
import type { IFilter } from "~/types/types";

const demoFilter: Ref<IFilter[]> = ref([
  {
    id: "search",
    config: {
      label: "Search in variables",
      type: "SEARCH",
      initialCollapsed: false,
    },
    search: "",
  },
  {
    id: "topics",
    config: {
      label: "Topics",
      type: "ONTOLOGY",
      ontologyTableId: "Keywords",
      ontologySchemaId: "CatalogueOntologies",
      columnId: "keywords",
      initialCollapsed: true,
    },
    conditions: [],
  },
  {
    id: "datasets",
    config: {
      label: "Datasets",
      type: "REF_ARRAY",
      refTableId: "Datasets",
      refSchemaId: "catalogue-demo",
      refLabel: "${resource.name} - ${name}",
      refDescription: "${description}",
      columnId: "datasets",
      initialCollapsed: true,
    },
    conditions: [],
  },
  // {
  //   id: "resources",
  //   config: {
  //     label: "Sources",
  //     type: "REF_ARRAY",
  //     refTableId: "Resources",
  //     refSchemaId: "catalogue-demo",
  //     refLabel: "${id}",
  //     refDescription: "${name}",
  //     initialCollapsed: false,
  //   },
  //   conditions: [],
  // },
]);
</script>

<template>
  <div class="grid grid-cols-[380px_auto] gap-4">
    <FilterSidebar
      id="test-sidebar"
      title="My filter sidebar"
      :filters="demoFilter"
    />
    <pre>value={{ JSON.stringify(demoFilter, null, 2) }}</pre>
  </div>
</template>
