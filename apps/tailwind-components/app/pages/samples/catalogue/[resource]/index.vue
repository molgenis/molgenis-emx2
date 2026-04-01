<template>
  <div>
    <div class="flex flex-wrap gap-2 p-4 bg-gray-100 rounded mb-4">
      <NuxtLink
        v-for="resource in resources"
        :key="resource.id"
        :to="`/samples/catalogue/${resource.id}`"
        class="px-3 py-1 rounded text-sm"
        :class="
          resource.id === route.params.resource
            ? 'bg-blue-600 text-white'
            : 'bg-white text-blue-600 hover:bg-blue-50'
        "
      >
        {{ resource.acronym || resource.id }}
      </NuxtLink>
    </div>
    <DetailView
      schema-id="catalogue-demo"
      table-id="Resources"
      :row-id="rowId"
      :show-side-nav="true"
      :expand-level="3"
      :row-transform="aggregateCollectionEvents"
      :column-transform="injectMergedColumns"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import { useAsyncData } from "#app";
import { navigateTo } from "#imports";
import type { IColumn } from "../../../../../../metadata-utils/src/types";
import DetailView from "../../../../components/display/DetailView.vue";
import fetchGraphql from "../../../../composables/fetchGraphql";
import { provideRecordNavigation } from "../../../../composables/useRecordNavigation";

const route = useRoute();

provideRecordNavigation({
  async navigateToRecord(schemaId, tableId, row) {
    const resource = route.params.resource as string;
    if (tableId === "CollectionEvents" && row.name) {
      await navigateTo(
        `/samples/catalogue/${resource}/collection-events/${encodeURIComponent(
          row.name
        )}`
      );
      return;
    }
    if (tableId === "Subpopulations" && row.name) {
      await navigateTo(
        `/samples/catalogue/${resource}/subpopulations/${encodeURIComponent(
          row.name
        )}`
      );
      return;
    }
  },
});

const rowId = computed(() => ({
  id: route.params.resource as string,
}));

const { data: resourceList } = useAsyncData("catalogue-resources", () =>
  fetchGraphql("catalogue-demo", `{ Resources { id, acronym } }`, undefined)
);

const resources = computed(() => resourceList.value?.Resources || []);

function mergeOntologyArrays(
  events: Record<string, any>[],
  field: string
): Record<string, any>[] {
  const seen = new Set<string>();
  const merged: Record<string, any>[] = [];
  for (const event of events) {
    const items = event[field];
    if (!Array.isArray(items)) continue;
    for (const item of items) {
      if (item?.name && !seen.has(item.name)) {
        seen.add(item.name);
        merged.push(item);
      }
    }
  }
  return merged;
}

function aggregateCollectionEvents(
  row: Record<string, any>
): Record<string, any> {
  const events = row.collectionEvents;
  if (!Array.isArray(events) || events.length === 0) return row;

  const merged = { ...row };

  const mergedAreas = mergeOntologyArrays(events, "areasOfInformation");
  if (mergedAreas.length > 0) {
    merged["areasOfInformation"] = mergedAreas;
  }

  const mergedSampleCategories = mergeOntologyArrays(
    events,
    "sampleCategories"
  );
  if (mergedSampleCategories.length > 0) {
    merged["biospecimenCollected"] = mergedSampleCategories;
    merged["_mergedSampleCategories"] = mergedSampleCategories;
  }

  const mergedDataCategories = mergeOntologyArrays(events, "dataCategories");
  if (mergedDataCategories.length > 0) {
    merged["_mergedDataCategories"] = mergedDataCategories;
  }

  return merged;
}

function injectMergedColumns(columns: IColumn[]): IColumn[] {
  const headingIndex = columns.findIndex(
    (c) => c.columnType === "HEADING" && c.id === "availableDataAndSamples"
  );

  if (headingIndex === -1) return columns;

  const headingId = columns[headingIndex]!.id;

  const virtualColumns: IColumn[] = [
    {
      id: "_mergedDataCategories",
      label: "Data categories",
      columnType: "ONTOLOGY_ARRAY",
      heading: headingId,
    } as IColumn,
    {
      id: "_mergedSampleCategories",
      label: "Sample categories",
      columnType: "ONTOLOGY_ARRAY",
      heading: headingId,
    } as IColumn,
  ];

  const result = [...columns];
  result.splice(headingIndex + 1, 0, ...virtualColumns);
  return result;
}
</script>
