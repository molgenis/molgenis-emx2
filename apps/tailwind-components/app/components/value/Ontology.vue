<script setup lang="ts">
import { ref, watch } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import fetchOntologyAncestry from "../../composables/fetchOntologyAncestry";
import type { IOntologyTreeItem } from "../../utils/buildOntologyTree";
import { resolveOntologyAncestry } from "../../utils/resolveOntologyAncestry";
import DisplayOntology from "../display/Ontology.vue";

const props = defineProps<{
  metadata: IColumn;
  data: IOntologyTreeItem | IOntologyTreeItem[];
}>();

// Shown immediately, then swapped for the ancestor-linked tree once it resolves.
// Avoids a blank record view while the ancestry request is in flight.
const resolvedValue = ref<IOntologyTreeItem | IOntologyTreeItem[]>(props.data);

watch(
  () => props.data,
  async (value) => {
    resolvedValue.value = value;
    const schemaId = props.metadata.refSchemaId;
    const tableId = props.metadata.refTableId;
    if (!value || !schemaId || !tableId) {
      return;
    }

    const values = Array.isArray(value) ? value : [value];
    const termNames = values.map((term) => term.name);
    try {
      const termsByName = await fetchOntologyAncestry(
        schemaId,
        tableId,
        termNames
      );
      resolvedValue.value = resolveOntologyAncestry(values, termsByName);
    } catch (err) {
      console.error("Failed to resolve ontology ancestry", err);
    }
  },
  { immediate: true }
);
</script>

<template>
  <DisplayOntology :value="resolvedValue" />
</template>
