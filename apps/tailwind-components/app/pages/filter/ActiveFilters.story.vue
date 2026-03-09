<script setup lang="ts">
import { ref } from "vue";
import ActiveFilters from "../../components/filter/ActiveFilters.vue";
import Story from "../../components/Story.vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";

const columns: IColumn[] = [
  {
    id: "name",
    label: "Name",
    columnType: "STRING",
    table: "Person",
    key: 1,
    required: false,
  },
  {
    id: "age",
    label: "Age",
    columnType: "INT",
    table: "Person",
    key: 1,
    required: false,
  },
  {
    id: "category",
    label: "Category",
    columnType: "REF_ARRAY",
    table: "Person",
    key: 1,
    required: false,
  },
  {
    id: "birthDate",
    label: "Birth Date",
    columnType: "DATE",
    table: "Person",
    key: 1,
    required: false,
  },
  {
    id: "email",
    label: "Email",
    columnType: "STRING",
    table: "Person",
    key: 1,
    required: false,
  },
];

const emptyFilters = ref<Map<string, IFilterValue>>(new Map());

const singleFilter = ref<Map<string, IFilterValue>>(
  new Map([["name", { operator: "like", value: "John" }]])
);

const multipleFilters = ref<Map<string, IFilterValue>>(
  new Map([
    ["name", { operator: "like", value: "John" }],
    ["age", { operator: "between", value: [18, 65] }],
    ["category", { operator: "in", value: [{ name: "A" }, { name: "B" }] }],
  ])
);

const rangeMinOnly = ref<Map<string, IFilterValue>>(
  new Map([["age", { operator: "between", value: [18, null] }]])
);

const rangeMaxOnly = ref<Map<string, IFilterValue>>(
  new Map([["age", { operator: "between", value: [null, 65] }]])
);

const nullFilters = ref<Map<string, IFilterValue>>(
  new Map([
    ["email", { operator: "isNull", value: true }],
    ["birthDate", { operator: "notNull", value: true }],
  ])
);

function handleRemove(columnId: string) {
  console.log("Remove filter:", columnId);
}

function handleClearAll() {
  console.log("Clear all filters");
}
</script>

<template>
  <Story title="ActiveFilters">
    <div class="space-y-8">
      <div>
        <h3 class="text-lg font-bold mb-2">Empty State</h3>
        <ActiveFilters
          :filters="emptyFilters"
          :columns="columns"
          @remove="handleRemove"
          @clear-all="handleClearAll"
        />
        <p class="text-sm text-gray-500 mt-2">
          (Nothing displayed when no filters)
        </p>
      </div>

      <div>
        <h3 class="text-lg font-bold mb-2">Single Filter (String/Like)</h3>
        <ActiveFilters
          :filters="singleFilter"
          :columns="columns"
          @remove="handleRemove"
          @clear-all="handleClearAll"
        />
        <p class="text-sm text-gray-500 mt-2">
          (No "Clear all" button with single filter)
        </p>
      </div>

      <div>
        <h3 class="text-lg font-bold mb-2">Multiple Filters</h3>
        <ActiveFilters
          :filters="multipleFilters"
          :columns="columns"
          @remove="handleRemove"
          @clear-all="handleClearAll"
        />
        <p class="text-sm text-gray-500 mt-2">
          (String like, range between, ref array in)
        </p>
      </div>

      <div>
        <h3 class="text-lg font-bold mb-2">Range Filters (Min/Max Only)</h3>
        <div class="space-y-4">
          <div>
            <p class="text-sm mb-2">Min only:</p>
            <ActiveFilters
              :filters="rangeMinOnly"
              :columns="columns"
              @remove="handleRemove"
              @clear-all="handleClearAll"
            />
          </div>
          <div>
            <p class="text-sm mb-2">Max only:</p>
            <ActiveFilters
              :filters="rangeMaxOnly"
              :columns="columns"
              @remove="handleRemove"
              @clear-all="handleClearAll"
            />
          </div>
        </div>
      </div>

      <div>
        <h3 class="text-lg font-bold mb-2">Null Filters</h3>
        <ActiveFilters
          :filters="nullFilters"
          :columns="columns"
          @remove="handleRemove"
          @clear-all="handleClearAll"
        />
        <p class="text-sm text-gray-500 mt-2">(isNull and notNull operators)</p>
      </div>
    </div>
  </Story>
</template>
