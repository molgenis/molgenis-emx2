<script setup lang="ts">
import ActiveFilters from "../../components/filter/ActiveFilters.vue";
import type { ActiveFilter } from "../../../../types/filters";
import Story from "../../components/Story.vue";

const emptyFilters: ActiveFilter[] = [];

const singleFilter: ActiveFilter[] = [
  { columnId: "name", label: "Name", displayValue: "John", values: [] },
];

const multipleFilters: ActiveFilter[] = [
  { columnId: "name", label: "Name", displayValue: "John", values: [] },
  { columnId: "age", label: "Age", displayValue: "18 - 65", values: [] },
  {
    columnId: "category",
    label: "Category",
    displayValue: "2",
    values: ["A", "B"],
  },
];

const rangeMinOnly: ActiveFilter[] = [
  { columnId: "age", label: "Age", displayValue: "≥ 18", values: [] },
];

const rangeMaxOnly: ActiveFilter[] = [
  { columnId: "age", label: "Age", displayValue: "≤ 65", values: [] },
];

const nullFilters: ActiveFilter[] = [
  { columnId: "email", label: "Email", displayValue: "is empty", values: [] },
  {
    columnId: "birthDate",
    label: "Birth Date",
    displayValue: "has value",
    values: [],
  },
];

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
          @remove="handleRemove"
          @clear-all="handleClearAll"
        />
      </div>

      <div>
        <h3 class="text-lg font-bold mb-2">Multiple Filters</h3>
        <ActiveFilters
          :filters="multipleFilters"
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
              @remove="handleRemove"
              @clear-all="handleClearAll"
            />
          </div>
          <div>
            <p class="text-sm mb-2">Max only:</p>
            <ActiveFilters
              :filters="rangeMaxOnly"
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
          @remove="handleRemove"
          @clear-all="handleClearAll"
        />
        <p class="text-sm text-gray-500 mt-2">(isNull and notNull operators)</p>
      </div>
    </div>
  </Story>
</template>
