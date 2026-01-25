<template>
  <div class="p-6 space-y-4">
    <h1 class="text-heading-2xl font-bold mb-6">Emx2DataView Demo</h1>

    <div class="flex gap-4 mb-4">
      <button
        @click="layout = 'list'"
        :class="[
          'px-4 py-2 rounded font-semibold',
          layout === 'list' ? 'bg-blue-500 text-white' : 'bg-gray-200',
        ]"
      >
        List
      </button>
      <button
        @click="layout = 'table'"
        :class="[
          'px-4 py-2 rounded font-semibold',
          layout === 'table' ? 'bg-blue-500 text-white' : 'bg-gray-200',
        ]"
      >
        Table
      </button>
      <button
        @click="layout = 'cards'"
        :class="[
          'px-4 py-2 rounded font-semibold',
          layout === 'cards' ? 'bg-blue-500 text-white' : 'bg-gray-200',
        ]"
      >
        Cards
      </button>
    </div>

    <Emx2DataView
      :schema-id="schemaId"
      :table-id="tableId"
      :layout="layout"
      :show-filters="true"
      filter-position="sidebar"
      :filterable-columns="['name', 'category', 'weight']"
      :show-search="true"
      :paging-limit="10"
      :visible-columns="['name', 'category', 'weight']"
    />

    <!-- Specification -->
    <section class="mt-12 p-6 bg-gray-50 rounded border">
      <h2 class="text-heading-lg font-semibold mb-4">Specification</h2>
      <div class="text-body-sm space-y-4">
        <p>
          Demo of unified Emx2DataView component combining data fetching, layout
          options, and filter support.
        </p>

        <h3 class="font-semibold">Features</h3>
        <ul class="list-disc pl-5">
          <li>Single component replaces FilterSidebar + ListView/TableEMX2</li>
          <li>Three layout modes: list, table, cards</li>
          <li>Integrated filter sidebar with debounced updates (300ms)</li>
          <li>Auto-computed filterable columns from metadata</li>
          <li>Search input with pagination</li>
          <li>Uses useTableData and useFilters composables</li>
        </ul>

        <h3 class="font-semibold">Props</h3>
        <ul class="list-disc pl-5">
          <li>
            <code>layout</code>: 'list' | 'table' | 'cards' (default: list)
          </li>
          <li><code>showFilters</code>: boolean (default: false)</li>
          <li>
            <code>filterPosition</code>: 'sidebar' | 'topbar' (default: sidebar)
          </li>
          <li>
            <code>filterableColumns</code>: string[] (optional filter list)
          </li>
          <li><code>visibleColumns</code>: string[] (for table layout)</li>
          <li><code>showSearch</code>: boolean (default: true)</li>
          <li><code>pagingLimit</code>: number (default: 10)</li>
          <li><code>rowLabel</code>: string template like "${name}"</li>
          <li>
            <code>displayOptions</code>: Record&lt;colId, {href?, onClick?}&gt;
          </li>
        </ul>

        <h3 class="font-semibold">Slots</h3>
        <ul class="list-disc pl-5">
          <li><code>#default</code>: Custom list item (props: row, label)</li>
          <li><code>#card</code>: Custom card content (props: row, label)</li>
        </ul>

        <h3 class="font-semibold">Test Checklist</h3>
        <ul class="list-disc pl-5">
          <li>Expand Name filter - type text - results filter</li>
          <li>Expand Category filter - select value - results update</li>
          <li>Set Weight min/max - between filter applies</li>
          <li>Clear individual filter - results refresh</li>
          <li>Switch between list/table/cards - same filters apply</li>
          <li>Search works in all layout modes</li>
          <li>Pagination updates correctly</li>
          <li>Filters debounce at 300ms (no immediate flicker)</li>
        </ul>

        <h3 class="font-semibold">Schema & Table</h3>
        <ul class="list-disc pl-5">
          <li>Schema: "pet store"</li>
          <li>Table: "Pet"</li>
          <li>
            Filterable columns: name (STRING), category (REF), weight (INT)
          </li>
        </ul>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import Emx2DataView from "../../components/display/Emx2DataView.vue";

const schemaId = "pet store";
const tableId = "Pet";
const layout = ref<"list" | "table" | "cards">("list");
</script>
