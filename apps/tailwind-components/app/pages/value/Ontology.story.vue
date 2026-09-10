<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type {
  IColumn,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import DemoDataControls from "../../DemoDataControls.vue";
import ValueEMX2 from "../../components/value/EMX2.vue";
import fetchGraphql from "../../composables/fetchGraphql";
import ValueOntology from "../../components/value/Ontology.vue";
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

const route = useRoute();
const router = useRouter();
const loadFromDatabase = ref(!!route.query.schema);
const schemaId = ref((route.query.schema as string) || "CatalogueOntologies");
const tableId = ref((route.query.table as string) || "Keywords");
const tableMetadata = ref<ITableMetaData>();
const allTerms = ref<IOntologyTreeItem[]>([]);
const loadError = ref("");
const compact = ref(false);
const collapseAll = ref(false);
const maxItems = ref<number | string>(10);
const itemStep = ref<number | string>(5);
const renderLimit = ref<number | string>(1000);

function atLeastOne(value: number | string) {
  return Math.max(1, Number(value) || 1);
}

watch([schemaId, tableId], ([schema, table]) => {
  router.push({ query: { schema, table } });
});

watch(
  [loadFromDatabase, schemaId, tableId],
  async ([load, schema, table]) => {
    allTerms.value = [];
    loadError.value = "";
    if (!load || !schema || !table) {
      return;
    }
    try {
      const data = await fetchGraphql(
        schema,
        `{ ${table}(limit: 100000) { name } }`,
        {}
      );
      // A reply for a table the reader has already left is dropped.
      if (schema !== schemaId.value || table !== tableId.value) {
        return;
      }
      allTerms.value = data?.[table] ?? [];
    } catch (err) {
      console.error("Failed to load ontology terms", err);
      loadError.value = `Could not load the terms of ${schema} › ${table}.`;
    }
  },
  { immediate: true }
);

const ontologyColumn = computed<IColumn>(() => ({
  id: "all-terms",
  label: "All terms",
  columnType: "ONTOLOGY_ARRAY",
  refSchemaId: schemaId.value,
  refTableId: tableId.value,
}));

const termCount = computed(() => allTerms.value.length);
</script>

<template>
  <div class="p-5 space-y-10">
    <p class="text-body-base">
      Displays ontology values as single item, flat list, or collapsible tree
      depending on data structure.
    </p>

    <div class="space-y-4">
      <h1 class="text-lg font-bold">From a database</h1>
      <p class="text-body-base">
        This part needs a running backend. Pick an ontology schema and table.
        The section loads every term of that table. By default it renders them
        as a record: the tree, with the ancestors it fetches. Compact renders
        them as a table cell does.
      </p>
      <div class="flex items-center gap-2">
        <InputCheckbox id="load-from-database" v-model="loadFromDatabase" />
        <InputLabel for="load-from-database">
          Load terms from a database
        </InputLabel>
      </div>
      <Suspense v-if="loadFromDatabase">
        <div
          class="p-6 rounded shadow-primary space-y-6 bg-content text-title-contrast"
        >
          <DemoDataControls
            v-model:metadata="tableMetadata"
            v-model:schemaId="schemaId"
            v-model:tableId="tableId"
          />
          <p v-if="loadError" class="text-invalid">{{ loadError }}</p>
          <p v-else class="text-body-sm">
            {{ termCount }} terms in {{ schemaId }} › {{ tableId }}
          </p>
          <div class="flex flex-wrap items-end gap-6">
            <div class="flex items-center gap-2">
              <InputCheckbox id="database-compact" v-model="compact" />
              <InputLabel for="database-compact">
                Compact, as in a table cell
              </InputLabel>
            </div>
            <div class="flex items-center gap-2">
              <InputCheckbox id="database-collapse-all" v-model="collapseAll" />
              <InputLabel for="database-collapse-all">Collapse all</InputLabel>
            </div>
            <div class="flex flex-col gap-1">
              <InputLabel for="database-max-items">Items per level</InputLabel>
              <InputInt
                id="database-max-items"
                v-model="maxItems"
                class="w-32"
              />
            </div>
            <div class="flex flex-col gap-1">
              <InputLabel for="database-item-step">Show more step</InputLabel>
              <InputInt
                id="database-item-step"
                v-model="itemStep"
                class="w-32"
              />
            </div>
            <div class="flex flex-col gap-1">
              <InputLabel for="database-render-limit">
                Values rendered at a time
              </InputLabel>
              <InputInt
                id="database-render-limit"
                v-model="renderLimit"
                class="w-32"
              />
            </div>
          </div>
          <ValueEMX2
            v-if="compact"
            :metadata="ontologyColumn"
            :data="allTerms"
            :renderLimit="atLeastOne(renderLimit)"
            compact
          />
          <ValueOntology
            v-else
            :metadata="ontologyColumn"
            :value="allTerms"
            :collapseAll="collapseAll"
            :maxItems="atLeastOne(maxItems)"
            :itemStep="atLeastOne(itemStep)"
            :renderLimit="atLeastOne(renderLimit)"
          />
        </div>
      </Suspense>
    </div>

    <div v-for="surface in surfaces" :key="surface.key" class="space-y-4">
      <h1 class="text-lg font-bold">{{ surface.label }}</h1>

      <div
        class="p-6 rounded shadow-primary space-y-6"
        :class="surface.wrapperClass"
      >
        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">Single Item</h2>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
            <span class="font-medium text-record-label">No definition:</span>
            <ValueOntology :value="singleItem" />
          </div>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
            <span class="font-medium text-record-label">With definition:</span>
            <ValueOntology :value="singleWithDefinition" />
          </div>
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">Flat List</h2>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
            <span class="font-medium text-record-label">No definitions:</span>
            <ValueOntology :value="flatList" />
          </div>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
            <span class="font-medium text-record-label">With definitions:</span>
            <ValueOntology :value="flatListWithDefinitions" />
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
            <ValueOntology :value="hierarchicalTree" :collapse-all="true" />
          </div>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-start">
            <span class="font-medium text-record-label"
              >Tree (root expanded):</span
            >
            <ValueOntology :value="hierarchicalTree" :collapse-all="false" />
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
              <ValueOntology :value="mixedRootTree" :collapse-all="false" />
            </div>
          </div>
        </div>

        <div class="space-y-4">
          <h2 class="text-xl font-semibold text-record-heading">Empty Value</h2>

          <div class="grid grid-cols-[200px_1fr] gap-2 items-center">
            <span class="font-medium text-record-label">Empty array:</span>
            <ValueOntology :value="[]" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
