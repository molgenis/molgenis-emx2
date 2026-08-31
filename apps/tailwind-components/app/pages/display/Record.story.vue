<template>
  <div class="flex flex-col gap-8 p-4">
    <section>
      <h2 class="text-title text-heading-xl mb-2">Static metadata</h2>
      <DisplayRecord :metadata="staticMetadata" :rowData="staticRowData" />
    </section>

    <section>
      <h2 class="text-title text-heading-xl mb-2">Live record</h2>

      <DemoDataControls
        v-model:metadata="metadata"
        v-model:schemaId="schemaId"
        v-model:formValues="formValues"
        v-model:tableId="tableId"
        :include-row-select="true"
        :row-index="rowIndex"
      >
      </DemoDataControls>

      <fieldset class="p-4 border-2 mb-2 flex flex-wrap gap-4">
        <legend class="text-title font-bold">Record props</legend>
        <div>
          <label class="text-title font-bold" for="show-legend">
            Legend (showLegend):
          </label>
          <InputCheckbox
            id="show-legend"
            v-model="showLegend"
            name="show-legend"
          />
        </div>
        <div>
          <label class="text-title font-bold" for="show-cards">
            Cards (showCards):
          </label>
          <InputCheckbox
            id="show-cards"
            v-model="showCards"
            name="show-cards"
          />
        </div>
        <div>
          <label class="text-title font-bold" for="show-filter">
            Field filter (showFilter):
          </label>
          <InputCheckbox
            id="show-filter"
            v-model="showFilter"
            name="show-filter"
          />
        </div>
        <div>
          <label class="text-title font-bold" for="show-mg-columns">
            mg_ columns (showMgColumns):
          </label>
          <InputCheckbox
            id="show-mg-columns"
            v-model="showMgColumns"
            name="show-mg-columns"
          />
        </div>
      </fieldset>

      <DisplayRecord
        v-if="metadata"
        :key="`${schemaId} - ${metadata.id} - ${JSON.stringify(formValues)}`"
        :metadata="metadata"
        :rowData="formValues"
        :showLegend="showLegend"
        :showCards="showCards"
        :showFilter="showFilter"
        :showMgColumns="showMgColumns"
      />
    </section>

    <section>
      <h2 class="text-title text-heading-xl mb-2">Live RecordKeyAccordion</h2>

      <fieldset class="p-4 border-2 mb-2 flex flex-wrap gap-4">
        <legend class="text-title font-bold">RecordKeyAccordion props</legend>
        <div>
          <label class="text-title font-bold" for="show-details">
            Details (showDetails):
          </label>
          <InputCheckbox
            id="show-details"
            v-model="showDetails"
            name="show-details"
          />
        </div>
        <div>
          <label class="text-title font-bold" for="open-by-default">
            Open by default (openByDefault):
          </label>
          <InputCheckbox
            id="open-by-default"
            v-model="openByDefault"
            name="open-by-default"
          />
        </div>
      </fieldset>

      <RecordKeyAccordion
        v-if="metadata"
        :key="`${schemaId} - ${metadata.id} - ${JSON.stringify(formValues)}`"
        :metadata="metadata"
        :row-data="formValues"
        :show-details="showDetails"
        :open-by-default="openByDefault"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import type {
  columnId,
  columnValue,
  IRow,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import DisplayRecord from "../../components/display/Record.vue";
import RecordKeyAccordion from "../../components/display/RecordKeyAccordion.vue";
import DemoDataControls from "../../DemoDataControls.vue";
import { useRoute, useRouter } from "vue-router";

const staticMetadata: ITableMetaData = {
  id: "Pet",
  schemaId: "pet store",
  name: "Pet",
  label: "Pet",
  tableType: "DATA",
  columns: [
    { id: "mg_top_of_form", label: "_top", columnType: "SECTION" },
    { id: "name", label: "Name", columnType: "STRING", key: 1 },
    { id: "about", label: "About", columnType: "SECTION" },
    { id: "category", label: "Category", columnType: "STRING" },
    { id: "size", label: "Size", columnType: "HEADING" },
    { id: "weight", label: "Weight", columnType: "DECIMAL" },
    { id: "height", label: "Height", columnType: "DECIMAL" },
    { id: "care", label: "Care", columnType: "SECTION" },
    { id: "diet", label: "Diet", columnType: "STRING" },
    { id: "notes", label: "Notes", columnType: "TEXT" },
  ],
};

const staticRowData: IRow = {
  name: "spike",
  category: "dog",
  weight: 15.7,
  height: null,
  diet: "insects",
  notes: "Sleeps a lot.",
};

const router = useRouter();
const route = useRoute();

const metadata = ref<ITableMetaData>();
const schemaId = ref<string>((route.query.schema as string) || "pet store");
const formValues = ref<Record<columnId, columnValue>>({});
const tableId = ref<string>((route.query.table as string) || "Pet");
const rowIndex = ref<number>(
  route.query.rowIndex ? Number(route.query.rowIndex) : 0
);

const showLegend = ref(true);
const showCards = ref(true);
const showFilter = ref(false);
const showMgColumns = ref(false);
const showDetails = ref(true);
const openByDefault = ref(false);

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});
</script>
