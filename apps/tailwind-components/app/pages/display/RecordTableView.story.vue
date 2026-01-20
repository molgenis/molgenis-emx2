<script setup lang="ts">
import { ref } from "vue";
import type { IColumn, IRefColumn } from "../../../../metadata-utils/src/types";
import DisplayRecordTableView from "../../components/display/RecordTableView.vue";

const clickLog = ref<string[]>([]);
const currentPage = ref(1);
const currentPageLarge = ref(1);

const petColumns: IColumn[] = [
  { id: "name", label: "Name", columnType: "STRING", key: 1 },
  { id: "species", label: "Species", columnType: "STRING" },
  { id: "age", label: "Age", columnType: "INT" },
  { id: "vaccinated", label: "Vaccinated", columnType: "BOOL" },
];

const orderColumns: IColumn[] = [
  { id: "orderId", label: "Order ID", columnType: "STRING", key: 1 },
  { id: "status", label: "Status", columnType: "STRING" },
  { id: "total", label: "Total", columnType: "DECIMAL" },
  { id: "orderDate", label: "Order Date", columnType: "DATE" },
];

const refColumn: IRefColumn = {
  id: "pets",
  label: "Pets",
  columnType: "REF_ARRAY",
  refTableId: "Pet",
  refSchemaId: "pet store",
  refLabel: "${name}",
  refLabelDefault: "${name}",
  refLinkId: "name",
};

const orderRefColumn: IRefColumn = {
  id: "orders",
  label: "Orders",
  columnType: "REF_ARRAY",
  refTableId: "Order",
  refSchemaId: "pet store",
  refLabel: "${orderId}",
  refLabelDefault: "${orderId}",
  refLinkId: "orderId",
};

const petRows = [
  { name: "Fluffy", species: "Cat", age: 3, vaccinated: true },
  { name: "Buddy", species: "Dog", age: 5, vaccinated: true },
  { name: "Max", species: "Dog", age: 2, vaccinated: false },
];

const largeOrderRows = Array.from({ length: 15 }, (_, i) => ({
  orderId: `ORD-${String(i + 1).padStart(3, "0")}`,
  status: ["Pending", "Shipped", "Delivered"][i % 3],
  total: (Math.random() * 100 + 10).toFixed(2),
  orderDate: `2024-0${(i % 9) + 1}-${String((i % 28) + 1).padStart(2, "0")}`,
}));

const pageSize = 5;

const visiblePetRows = petRows;
const visibleOrderRows = computed(() =>
  largeOrderRows.slice(
    (currentPageLarge.value - 1) * pageSize,
    currentPageLarge.value * pageSize
  )
);

import { computed } from "vue";

function getRefClickAction(col: IColumn, row: any) {
  return () => {
    const message = `Clicked: column="${col.id}", row=${JSON.stringify(row)}`;
    clickLog.value.unshift(message);
    if (clickLog.value.length > 5) {
      clickLog.value.pop();
    }
  };
}

function clearLog() {
  clickLog.value = [];
}
</script>

<template>
  <div class="p-5 space-y-6">
    <h1 class="text-2xl font-bold">RecordTableView Component</h1>
    <p class="text-gray-600">
      Renders REF_ARRAY/REFBACK data as a table with configurable columns.
    </p>

    <div
      class="bg-content p-6 rounded shadow-primary space-y-8 text-record-value"
    >
      <div class="space-y-4">
        <h2 class="text-xl font-semibold text-record-heading">
          Basic Table (3 items, no pagination)
        </h2>

        <DisplayRecordTableView
          :rows="visiblePetRows"
          :columns="petColumns"
          :ref-column="refColumn"
          :page="currentPage"
          :page-size="pageSize"
          :total-count="petRows.length"
          :get-ref-click-action="getRefClickAction"
          @update:page="currentPage = $event"
        />
      </div>

      <div class="space-y-4">
        <h2 class="text-xl font-semibold text-record-heading">
          Paginated Table (15 items)
        </h2>

        <DisplayRecordTableView
          :rows="visibleOrderRows"
          :columns="orderColumns"
          :ref-column="orderRefColumn"
          :page="currentPageLarge"
          :page-size="pageSize"
          :total-count="largeOrderRows.length"
          :get-ref-click-action="getRefClickAction"
          @update:page="currentPageLarge = $event"
        />
      </div>

      <div class="space-y-4">
        <h2 class="text-xl font-semibold text-record-heading">Empty Table</h2>

        <DisplayRecordTableView
          :rows="[]"
          :columns="petColumns"
          :ref-column="refColumn"
          :page="1"
          :page-size="pageSize"
          :total-count="0"
        />
      </div>

      <div class="mt-4 p-4 bg-gray-50 rounded border">
        <div class="flex justify-between items-center mb-2">
          <span class="font-medium text-record-label">Click Event Log:</span>
          <button
            class="text-sm text-blue-600 hover:underline"
            @click="clearLog"
          >
            Clear
          </button>
        </div>
        <div v-if="clickLog.length === 0" class="text-gray-400 italic">
          No clicks yet
        </div>
        <ul v-else class="space-y-1 text-sm font-mono">
          <li
            v-for="(log, index) in clickLog"
            :key="index"
            class="text-gray-700"
          >
            {{ log }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>
