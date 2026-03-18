<template>
  <Story title="useFilters">
    <div class="p-6 space-y-6 max-w-4xl">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="space-y-4">
          <h2 class="text-heading-lg font-semibold">Set Filters</h2>

          <div class="space-y-2">
            <label class="block text-body-sm font-medium">
              Name (STRING - like)
            </label>
            <div class="flex gap-2">
              <input
                v-model="nameInput"
                class="border rounded px-3 py-1.5 text-body-sm flex-1"
                placeholder="e.g. John"
              />
              <button
                @click="setNameFilterAction"
                class="px-3 py-1.5 bg-blue-500 text-white rounded text-body-sm"
              >
                Set
              </button>
            </div>
          </div>

          <div class="space-y-2">
            <label class="block text-body-sm font-medium">
              Age (INT - between)
            </label>
            <div class="flex gap-2 items-center">
              <input
                v-model.number="ageMin"
                type="number"
                class="border rounded px-3 py-1.5 text-body-sm w-24"
                placeholder="min"
              />
              <span class="text-body-sm">to</span>
              <input
                v-model.number="ageMax"
                type="number"
                class="border rounded px-3 py-1.5 text-body-sm w-24"
                placeholder="max"
              />
              <button
                @click="setAgeFilterAction"
                class="px-3 py-1.5 bg-blue-500 text-white rounded text-body-sm"
              >
                Set
              </button>
            </div>
          </div>

          <div class="space-y-2">
            <label class="block text-body-sm font-medium">
              Category (REF - in)
            </label>
            <div class="flex gap-2 flex-wrap">
              <button
                v-for="cat in availableCategories"
                :key="cat"
                @click="toggleCategoryAction(cat)"
                :class="[
                  'px-3 py-1.5 rounded text-body-sm border',
                  selectedCategories.includes(cat)
                    ? 'bg-green-500 text-white border-green-500'
                    : 'bg-white border-gray-300',
                ]"
              >
                {{ cat }}
              </button>
            </div>
          </div>

          <div class="space-y-2">
            <label class="block text-body-sm font-medium">Global search</label>
            <div class="flex gap-2">
              <input
                v-model="searchInput"
                class="border rounded px-3 py-1.5 text-body-sm flex-1"
                placeholder="Search all columns..."
              />
              <button
                @click="setSearchAction"
                class="px-3 py-1.5 bg-blue-500 text-white rounded text-body-sm"
              >
                Set
              </button>
            </div>
          </div>

          <div class="flex gap-2 flex-wrap pt-2">
            <button
              @click="clearNameAction"
              class="px-3 py-1.5 bg-gray-200 rounded text-body-sm"
            >
              Remove name filter
            </button>
            <button
              @click="clearAllAction"
              class="px-3 py-1.5 bg-red-100 text-red-700 rounded text-body-sm"
            >
              Clear all filters
            </button>
          </div>
        </div>

        <div class="space-y-4">
          <h2 class="text-heading-lg font-semibold">Current State</h2>

          <div class="p-4 bg-gray-50 rounded border">
            <h3 class="font-semibold text-body-sm mb-2">
              filterStates ({{ filterStates.size }} active)
            </h3>
            <pre
              class="text-body-xs font-mono whitespace-pre-wrap overflow-auto max-h-48"
              >{{ filterStatesJson }}</pre
            >
          </div>

          <div class="p-4 bg-gray-50 rounded border">
            <h3 class="font-semibold text-body-sm mb-2">searchValue</h3>
            <pre class="text-body-xs font-mono">{{
              searchValue || "(empty)"
            }}</pre>
          </div>

          <div class="p-4 bg-blue-50 rounded border border-blue-200">
            <h3 class="font-semibold text-body-sm mb-2">
              gqlFilter (debounced 300ms)
            </h3>
            <pre
              class="text-body-xs font-mono whitespace-pre-wrap overflow-auto max-h-48"
              >{{ gqlFilterJson }}</pre
            >
          </div>

          <div class="p-4 bg-green-50 rounded border border-green-200">
            <h3 class="font-semibold text-body-sm mb-2">
              Serialized URL params (live)
            </h3>
            <table class="text-body-xs font-mono w-full">
              <tbody>
                <tr
                  v-for="[param, value] in urlParamEntries"
                  :key="param"
                  class="border-t border-green-100"
                >
                  <td class="py-0.5 pr-3 text-green-700 whitespace-nowrap">
                    ?{{ param }}=
                  </td>
                  <td class="py-0.5 break-all">{{ value }}</td>
                </tr>
                <tr v-if="urlParamEntries.length === 0">
                  <td class="py-0.5 opacity-50 italic">(no active filters)</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="p-4 bg-gray-50 rounded border">
        <h3 class="font-semibold text-body-sm mb-2">Mock Columns</h3>
        <div class="flex gap-4 flex-wrap">
          <div
            v-for="col in mockColumns"
            :key="col.id"
            class="font-mono text-body-xs"
          >
            <span class="text-gray-500">{{ col.columnType }}</span>
            <span class="ml-1 font-semibold">{{ col.id }}</span>
          </div>
        </div>
      </div>
    </div>
  </Story>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import {
  useFilters,
  serializeFiltersToUrl,
} from "../../composables/useFilters";

const mockColumns = ref<IColumn[]>([
  { id: "name", label: "Name", columnType: "STRING", position: 1 },
  { id: "age", label: "Age", columnType: "INT", position: 2 },
  { id: "category", label: "Category", columnType: "REF", position: 3 },
] as IColumn[]);

const {
  filterStates,
  searchValue,
  gqlFilter,
  setFilter,
  setSearch,
  clearFilters,
  removeFilter,
} = useFilters(mockColumns);

const nameInput = ref("");
const ageMin = ref<number | null>(null);
const ageMax = ref<number | null>(null);
const searchInput = ref("");
const availableCategories = ["Cat1", "Cat2", "Cat3"];
const selectedCategories = ref<string[]>([]);

const filterStatesJson = computed(() => {
  const obj: Record<string, unknown> = {};
  for (const [key, val] of filterStates.value) {
    obj[key] = val;
  }
  return JSON.stringify(obj, null, 2);
});

const gqlFilterJson = computed(() => JSON.stringify(gqlFilter.value, null, 2));

const urlParamEntries = computed(() => {
  const params = serializeFiltersToUrl(
    filterStates.value,
    searchValue.value,
    mockColumns.value
  );
  return Object.entries(params);
});

function setNameFilterAction() {
  if (!nameInput.value.trim()) return;
  setFilter("name", { operator: "like", value: nameInput.value.trim() });
}

function setAgeFilterAction() {
  if (ageMin.value === null && ageMax.value === null) return;
  setFilter("age", {
    operator: "between",
    value: [ageMin.value ?? null, ageMax.value ?? null],
  });
}

function toggleCategoryAction(cat: string) {
  const idx = selectedCategories.value.indexOf(cat);
  if (idx === -1) {
    selectedCategories.value.push(cat);
  } else {
    selectedCategories.value.splice(idx, 1);
  }
  if (selectedCategories.value.length === 0) {
    removeFilter("category");
  } else {
    setFilter("category", {
      operator: "in",
      value: selectedCategories.value.map((name) => ({ name })),
    });
  }
}

function setSearchAction() {
  setSearch(searchInput.value);
}

function clearNameAction() {
  removeFilter("name");
  nameInput.value = "";
}

function clearAllAction() {
  clearFilters();
  nameInput.value = "";
  ageMin.value = null;
  ageMax.value = null;
  searchInput.value = "";
  selectedCategories.value = [];
}
</script>
