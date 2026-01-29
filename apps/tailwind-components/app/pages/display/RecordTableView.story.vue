<script setup lang="ts">
import { ref, computed } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import DisplayRecordTableView from "../../components/display/RecordTableView.vue";
import InlinePagination from "../../components/display/InlinePagination.vue";

const currentPage = ref(1);
const pageSize = 5;

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

const visibleOrderRows = computed(() =>
  largeOrderRows.slice(
    (currentPage.value - 1) * pageSize,
    currentPage.value * pageSize
  )
);

const totalPages = computed(() => Math.ceil(largeOrderRows.length / pageSize));
</script>

<template>
  <div class="p-5 space-y-6">
    <h1 class="text-2xl font-bold">RecordTableView Component</h1>
    <p class="text-gray-600">
      Simple table display for rows and columns. Parent handles pagination.
    </p>

    <div
      class="bg-content p-6 rounded shadow-primary space-y-8 text-record-value"
    >
      <div class="space-y-4">
        <h2 class="text-xl font-semibold text-record-heading">
          Basic Table (3 items)
        </h2>

        <DisplayRecordTableView :rows="petRows" :columns="petColumns" />
      </div>

      <div class="space-y-4">
        <h2 class="text-xl font-semibold text-record-heading">
          Paginated Table (15 items, pagination handled by parent)
        </h2>

        <DisplayRecordTableView
          :rows="visibleOrderRows"
          :columns="orderColumns"
        />
        <InlinePagination
          :current-page="currentPage"
          :total-pages="totalPages"
          @update:page="currentPage = $event"
        />
      </div>

      <div class="space-y-4">
        <h2 class="text-xl font-semibold text-record-heading">Empty Table</h2>

        <DisplayRecordTableView :rows="[]" :columns="petColumns" />
      </div>
    </div>
  </div>
</template>
