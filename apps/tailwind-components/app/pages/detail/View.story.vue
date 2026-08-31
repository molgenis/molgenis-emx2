<template>
  <div class="flex flex-col gap-8 p-4">
    <section>
      <h2 class="text-title text-heading-xl mb-2">Static metadata</h2>
      <DetailView :metadata="staticMetadata" :rowData="staticRowData" />
    </section>

    <section>
      <h2 class="text-title text-heading-xl mb-2">showCards off</h2>
      <DetailView
        :metadata="staticMetadata"
        :rowData="staticRowData"
        :showCards="false"
      />
    </section>

    <section>
      <h2 class="text-title text-heading-xl mb-2">showFilter on</h2>
      <DetailView
        :metadata="staticMetadata"
        :rowData="staticRowData"
        :showFilter="true"
      />
    </section>

    <section>
      <h2 class="text-title text-heading-xl mb-2">
        collapsed, showing the key columns with a control to expand
      </h2>
      <DetailView
        :metadata="staticMetadata"
        :rowData="staticRowData"
        :collapsed="true"
      />
    </section>

    <section>
      <h2 class="text-title text-heading-xl mb-2">Live record</h2>
      <DetailView
        v-if="metadata"
        :key="`${schemaId} - ${metadata.id} - ${JSON.stringify(formValues)}`"
        :metadata="metadata"
        :rowData="formValues"
      />

      <DemoDataControls
        v-model:metadata="metadata"
        v-model:schemaId="schemaId"
        v-model:formValues="formValues"
        v-model:tableId="tableId"
        :include-row-select="true"
        :row-index="rowIndex"
      >
      </DemoDataControls>
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
import DetailView from "../../components/detail/View.vue";
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

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});
</script>
