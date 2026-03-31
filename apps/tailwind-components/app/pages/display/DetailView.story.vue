<script setup lang="ts">
import { ref, watch, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import type {
  IColumn,
  ITableMetaData,
  IRefColumn,
} from "../../../../metadata-utils/src/types";
import DemoDataControls from "../../DemoDataControls.vue";
import DetailView from "../../components/display/DetailView.vue";

const router = useRouter();
const route = useRoute();

const schemaId = ref<string>((route.query.schema as string) || "pet store");
const tableId = ref<string>((route.query.table as string) || "Pet");
const metadata = ref<ITableMetaData>();
const formValues = ref<Record<string, any>>({});

const showEmpty = ref(false);
const useMockData = ref(false);
const viewColumnsInput = ref("");

const viewColumns = computed(() => {
  if (!viewColumnsInput.value.trim()) return undefined;
  return viewColumnsInput.value
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
});

const columnTransform = computed(() => {
  if (!viewColumns.value?.length) return undefined;
  return (columns: IColumn[]) => {
    const viewColumnSet = new Set(viewColumns.value);
    return columns.filter((col) => viewColumnSet.has(col.id));
  };
});

const rowId = computed(() => {
  if (!metadata.value || !formValues.value) return {};
  const keyColumns = metadata.value.columns?.filter((c) => c.key === 1) || [];
  const result: Record<string, any> = {};
  for (const col of keyColumns) {
    if (formValues.value[col.id] !== undefined) {
      result[col.id] = formValues.value[col.id];
    }
  }
  return result;
});

const hasRowId = computed(() => Object.keys(rowId.value).length > 0);

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});

const mockColumns: IColumn[] = [
  { id: "id", label: "ID", columnType: "AUTO_ID" },
  { id: "name", label: "Name", columnType: "STRING", key: 1 },

  {
    id: "generalSection",
    label: "General Information",
    columnType: "SECTION",
    description: "Basic details about the pet",
  },
  {
    id: "species",
    label: "Species",
    columnType: "ONTOLOGY",
    section: "generalSection",
  },
  {
    id: "breed",
    label: "Breed",
    columnType: "STRING",
    section: "generalSection",
  },
  {
    id: "birthDate",
    label: "Birth Date",
    columnType: "DATE",
    section: "generalSection",
  },

  {
    id: "physicalHeading",
    label: "Physical Characteristics",
    columnType: "HEADING",
    section: "generalSection",
  },
  {
    id: "weight",
    label: "Weight (kg)",
    columnType: "DECIMAL",
    heading: "physicalHeading",
  },
  {
    id: "color",
    label: "Color",
    columnType: "STRING",
    heading: "physicalHeading",
  },

  {
    id: "ownerSection",
    label: "Owner Information",
    columnType: "SECTION",
    description: "Details about the pet owner",
  },
  {
    id: "owner",
    label: "Owner",
    columnType: "REF",
    section: "ownerSection",
    refTableId: "Owner",
    refSchemaId: "pet store",
    refLabel: "${firstName} ${lastName}",
    refLabelDefault: "${firstName}",
    refLinkId: "id",
  } as IRefColumn,
  {
    id: "ownerContact",
    label: "Contact Email",
    columnType: "EMAIL",
    section: "ownerSection",
  },

  { id: "relatedSection", label: "Related Records", columnType: "SECTION" },
  {
    id: "orders",
    label: "Orders",
    columnType: "REF_ARRAY",
    section: "relatedSection",
    refTableId: "Order",
    refSchemaId: "pet store",
    refLabel: "${orderId} - ${status}",
    refLabelDefault: "${orderId}",
    refLinkId: "orderId",
  } as IRefColumn,
  {
    id: "tags",
    label: "Tags",
    columnType: "ONTOLOGY_ARRAY",
    section: "relatedSection",
  },
];

const mockRow = {
  id: "PET-001",
  name: "Fluffy",
  species: { name: "Dog", definition: "A domesticated carnivorous mammal" },
  breed: "Golden Retriever",
  birthDate: "2021-03-15",
  weight: 28.5,
  color: "Golden",
  owner: { firstName: "John", lastName: "Doe", id: 1 },
  ownerContact: "john.doe@example.com",
  orders: Array.from({ length: 8 }, (_, i) => ({
    orderId: `ORD-${String(i + 1).padStart(3, "0")}`,
    status: ["Pending", "Shipped", "Delivered"][i % 3],
  })),
  tags: [
    { name: "Friendly", definition: "Good with people and other pets" },
    { name: "Trained", definition: "Has basic obedience training" },
    { name: "Vaccinated" },
  ],
};

const mockRowWithEmpty = {
  ...mockRow,
  breed: null,
  weight: undefined,
  ownerContact: "",
  tags: [],
};
</script>

<template>
  <div class="p-5 space-y-8">
    <h1 class="text-2xl font-bold">DetailView Component</h1>
    <p class="text-gray-600 dark:text-gray-400">
      Renders a complete record with two-level hierarchy: SECTION > HEADING >
      columns. Smart mode fetches data from EMX2 backend; dumb mode renders
      provided data directly.
    </p>

    <div class="space-y-4 p-4 bg-gray-100 dark:bg-gray-800 rounded">
      <DemoDataControls
        v-model:metadata="metadata"
        v-model:schemaId="schemaId"
        v-model:tableId="tableId"
        v-model:formValues="formValues"
        :include-row-select="true"
        :row-index="1"
      />

      <div class="flex flex-col gap-2">
        <label for="viewColumns" class="text-title font-bold">
          View Columns (comma-separated, leave empty for all):
        </label>
        <input
          id="viewColumns"
          v-model="viewColumnsInput"
          type="text"
          class="border border-black p-2 dark:bg-gray-700 dark:border-gray-600"
          placeholder="e.g., name, species, breed"
        />
      </div>

      <div class="flex items-center gap-4">
        <div class="flex items-center gap-2">
          <input id="showEmpty" v-model="showEmpty" type="checkbox" />
          <label for="showEmpty">Show empty values</label>
        </div>
        <div class="flex items-center gap-2">
          <input id="useMock" v-model="useMockData" type="checkbox" />
          <label for="useMock">Use mock data (offline mode)</label>
        </div>
      </div>
    </div>

    <div v-if="!useMockData" class="space-y-4">
      <h2 class="text-xl font-semibold">Live Backend Data (smart mode)</h2>

      <div v-if="!hasRowId" class="p-4 border rounded dark:border-gray-600">
        <p class="text-gray-500 italic">
          Select a row above to view its details.
        </p>
      </div>

      <div v-else class="p-4 border rounded dark:border-gray-600">
        <DetailView
          :key="`${schemaId}-${tableId}-${JSON.stringify(rowId)}`"
          :schema-id="schemaId"
          :table-id="tableId"
          :row-id="rowId"
          :show-empty="showEmpty"
          :column-transform="columnTransform"
        >
          <template #header>
            <div class="mb-6 pb-4 border-b dark:border-gray-600">
              <h1 class="text-3xl font-bold">{{ tableId }} Record</h1>
              <p class="text-gray-500">Row ID: {{ JSON.stringify(rowId) }}</p>
            </div>
          </template>
          <template #footer>
            <div
              class="mt-6 pt-4 border-t dark:border-gray-600 text-sm text-gray-500"
            >
              Schema: {{ schemaId }} | Table: {{ tableId }}
            </div>
          </template>
        </DetailView>
      </div>
    </div>

    <div v-else class="space-y-4">
      <h2 class="text-xl font-semibold">Mock Data (dumb mode)</h2>
      <p class="text-sm text-gray-500">
        Using static mock data. Uncheck "Use mock data" to connect to backend.
      </p>

      <div class="space-y-4">
        <h3 class="text-lg font-semibold">Complete Record View</h3>
        <div class="p-4 border rounded dark:border-gray-600">
          <DetailView
            :columns="mockColumns"
            :data="mockRow"
            :show-empty="showEmpty"
          >
            <template #header>
              <div class="mb-6 pb-4 border-b dark:border-gray-600">
                <h1 class="text-3xl font-bold">{{ mockRow.name }}</h1>
                <p class="text-gray-500">Mock Pet Record</p>
              </div>
            </template>
            <template #footer>
              <div
                class="mt-6 pt-4 border-t dark:border-gray-600 text-sm text-gray-500"
              >
                Footer slot example
              </div>
            </template>
          </DetailView>
        </div>
      </div>

      <div class="space-y-4">
        <h3 class="text-lg font-semibold">Record with Empty Values</h3>
        <p class="text-sm text-gray-500">
          Toggle "Show empty values" above to see hidden columns.
        </p>
        <div class="p-4 border rounded dark:border-gray-600">
          <DetailView
            :columns="mockColumns"
            :data="mockRowWithEmpty"
            :show-empty="showEmpty"
          />
        </div>
      </div>
    </div>
  </div>
</template>
