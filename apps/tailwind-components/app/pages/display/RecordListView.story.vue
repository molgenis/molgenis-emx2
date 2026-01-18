<script setup lang="ts">
import { ref, computed } from "vue";
import type { IRefColumn, IRow } from "../../../../metadata-utils/src/types";
import RecordListView from "../../components/display/RecordListView.vue";

const clickLog = ref<string[]>([]);

const refColumn: IRefColumn = {
  id: "orders",
  label: "Orders",
  columnType: "REF_ARRAY",
  refTableId: "Order",
  refSchemaId: "pet store",
  refLabel: "${orderId} - ${status}",
  refLabelDefault: "${orderId}",
  refLinkId: "orderId",
};

// Small list (no pagination)
const smallRows: IRow[] = [
  { orderId: "ORD-001", status: "Pending", quantity: 2 },
  { orderId: "ORD-002", status: "Shipped", quantity: 1 },
  { orderId: "ORD-003", status: "Delivered", quantity: 3 },
];

// Large list (with pagination)
const largeRows: IRow[] = Array.from({ length: 15 }, (_, i) => ({
  orderId: `ORD-${String(i + 1).padStart(3, "0")}`,
  status: ["Pending", "Shipped", "Delivered"][i % 3],
  quantity: (i % 5) + 1,
}));

const pageSmall = ref(1);
const pageLarge = ref(1);
const pageSize = 5;

const visibleSmallRows = computed(() => {
  const start = (pageSmall.value - 1) * pageSize;
  return smallRows.slice(start, start + pageSize);
});

const visibleLargeRows = computed(() => {
  const start = (pageLarge.value - 1) * pageSize;
  return largeRows.slice(start, start + pageSize);
});

function getRefClickAction(col: any, row: IRow) {
  return () => {
    const message = `Clicked: ${col.id} -> ${JSON.stringify(row)}`;
    clickLog.value.unshift(message);
    if (clickLog.value.length > 5) clickLog.value.pop();
  };
}

function clearLog() {
  clickLog.value = [];
}
</script>

<template>
  <div class="p-5 space-y-8">
    <h1 class="text-2xl font-bold">RecordListView Component</h1>
    <p class="text-gray-600">
      Displays a list of referenced records with optional pagination.
    </p>

    <!-- Click log -->
    <div class="p-4 bg-gray-50 dark:bg-gray-800 rounded border">
      <div class="flex justify-between items-center mb-2">
        <span class="font-medium">Click Event Log:</span>
        <button class="text-sm text-blue-600 hover:underline" @click="clearLog">
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
          class="text-gray-700 dark:text-gray-300"
        >
          {{ log }}
        </li>
      </ul>
    </div>

    <!-- Small list (no pagination needed) -->
    <div class="space-y-4">
      <h2 class="text-xl font-semibold">Small List (3 items, no pagination)</h2>
      <div class="p-4 border rounded dark:border-gray-600">
        <RecordListView
          :rows="visibleSmallRows"
          :ref-column="refColumn"
          v-model:page="pageSmall"
          :page-size="pageSize"
          :total-count="smallRows.length"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>

    <!-- Large list (with pagination) -->
    <div class="space-y-4">
      <h2 class="text-xl font-semibold">
        Large List (15 items, with pagination)
      </h2>
      <p class="text-sm text-gray-500">
        Current page: {{ pageLarge }} | Total pages:
        {{ Math.ceil(largeRows.length / pageSize) }}
      </p>
      <div class="p-4 border rounded dark:border-gray-600">
        <RecordListView
          :rows="visibleLargeRows"
          :ref-column="refColumn"
          v-model:page="pageLarge"
          :page-size="pageSize"
          :total-count="largeRows.length"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>

    <!-- Empty list -->
    <div class="space-y-4">
      <h2 class="text-xl font-semibold">Empty List</h2>
      <div class="p-4 border rounded dark:border-gray-600">
        <RecordListView
          :rows="[]"
          :ref-column="refColumn"
          :page="1"
          :page-size="pageSize"
          :total-count="0"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>

    <!-- Dark mode test section -->
    <div class="space-y-4">
      <h2 class="text-xl font-semibold">Dark Mode Test</h2>
      <p class="text-sm text-gray-500">
        Toggle dark mode in your browser/OS to verify styling.
      </p>
      <div
        class="p-4 border rounded bg-white dark:bg-gray-900 dark:border-gray-600"
      >
        <RecordListView
          :rows="visibleSmallRows"
          :ref-column="refColumn"
          v-model:page="pageSmall"
          :page-size="pageSize"
          :total-count="smallRows.length"
          :get-ref-click-action="getRefClickAction"
        />
      </div>
    </div>
  </div>
</template>
