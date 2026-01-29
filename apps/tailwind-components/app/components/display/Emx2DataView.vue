<script setup lang="ts">
import { ref, computed, watch, useId } from "vue";
import { useRoute, useRouter } from "vue-router";
import type {
  IColumn,
  IRow,
  IDisplayConfig,
} from "../../../../metadata-utils/src/types";
import { useTableData } from "../../composables/useTableData";
import { useFilters } from "../../composables/useFilters";
import { rowToString } from "../../utils/rowToString";
import InputSearch from "../input/Search.vue";
import InlinePagination from "./InlinePagination.vue";
import LoadingContent from "../LoadingContent.vue";
import CardList from "../CardList.vue";
import CardListItem from "../CardListItem.vue";
import FilterSidebar from "../filter/Sidebar.vue";
import ActiveFilters from "../filter/ActiveFilters.vue";
import DetailPageLayout from "../layout/DetailPageLayout.vue";
import SideModal from "../SideModal.vue";
import ContentBlockModal from "../content/ContentBlockModal.vue";
import Button from "../Button.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    config?: IDisplayConfig;
    urlSync?: boolean;
  }>(),
  {
    urlSync: true,
  }
);

defineSlots<{
  header?: () => any;
  default?: (props: { row: IRow; label: string }) => any;
  card?: (props: { row: IRow; label: string }) => any;
}>();

const layout = computed(() => props.config?.layout || "table");
const showFilters = computed(() => props.config?.showFilters || false);
const filterPosition = computed(
  () => props.config?.filterPosition || "sidebar"
);
const filterableColumns = computed(() => props.config?.filterableColumns);
const showSearch = computed(() => props.config?.showSearch !== false);
const pagingLimit = computed(() => props.config?.pageSize || 10);
const rowLabel = computed(() => props.config?.rowLabel);
const visibleColumns = computed(() => props.config?.visibleColumns);
const displayOptions = computed(() => props.config?.columnConfig);

const searchInputId = useId();
const page = ref(1);
const searchTerms = ref("");

const route = useRoute();
const router = useRouter();

// compute filterable columns from metadata (initial empty state)
const metadataRef = ref<IColumn[]>([]);

const filterableColumnsComputed = computed<IColumn[]>(() => {
  const filterableTypes = [
    "STRING",
    "TEXT",
    "EMAIL",
    "INT",
    "DECIMAL",
    "LONG",
    "NON_NEGATIVE_INT",
    "DATE",
    "DATETIME",
    "BOOL",
    "REF",
    "REF_ARRAY",
    "ONTOLOGY",
    "ONTOLOGY_ARRAY",
  ];

  let cols = metadataRef.value.filter(
    (col) =>
      filterableTypes.includes(col.columnType) && !col.id.startsWith("mg_")
  );

  if (filterableColumns.value && filterableColumns.value.length > 0) {
    cols = cols.filter((col) => filterableColumns.value!.includes(col.id));
  }

  return cols;
});

const columnsRef = computed(() => filterableColumnsComputed.value);
const { filterStates, gqlFilter } = useFilters(columnsRef, {
  debounceMs: 300,
  urlSync: props.urlSync,
  route: route as any,
  router: router as any,
});

// fetch data using useTableData with filter
const { metadata, rows, status, totalPages, showPagination, errorMessage } =
  useTableData(props.schemaId, props.tableId, {
    pageSize: pagingLimit.value,
    page,
    searchTerms,
    filter: gqlFilter,
  });

// update metadataRef when metadata loads
watch(
  metadata,
  (newMeta) => {
    if (newMeta?.columns) {
      metadataRef.value = newMeta.columns;
    }
  },
  { immediate: true }
);

const errorText = computed(
  () =>
    errorMessage.value ||
    (status.value === "error" ? "Failed to load data" : undefined)
);

// row label template from props or metadata
const rowLabelTemplate = computed(() => {
  if (rowLabel.value) return rowLabel.value;
  const keyCol = metadata.value?.columns?.find((c) => c.key === 1);
  return keyCol?.refLabel || keyCol?.refLabelDefault || "${name}";
});

const visibleColumnsComputed = computed(() => {
  if (!metadata.value?.columns) return [];
  if (visibleColumns.value && visibleColumns.value.length > 0) {
    return visibleColumns.value.filter((colId) =>
      metadata.value!.columns!.some((c) => c.id === colId)
    );
  }
  return metadata.value.columns
    .filter((c) => !c.id.startsWith("mg_"))
    .map((c) => c.id);
});

const tableColumns = computed<IColumn[]>(() => {
  if (!metadata.value?.columns) return [];
  return visibleColumnsComputed.value
    .map((colId) => metadata.value!.columns!.find((c) => c.id === colId))
    .filter((col): col is IColumn => col !== undefined);
});

function getCellValue(row: IRow, colId: string): string {
  const val = row[colId];
  if (val === null || val === undefined) return "";
  if (Array.isArray(val)) {
    return val
      .map((v) => (typeof v === "object" ? v.label || v.name || "" : String(v)))
      .join(", ");
  }
  if (typeof val === "object") {
    const obj = val as Record<string, unknown>;
    return String(obj.label || obj.name || JSON.stringify(val));
  }
  return String(val);
}

watch(searchTerms, () => {
  page.value = 1;
});

function getLabel(row: IRow): string {
  return rowToString(row, rowLabelTemplate.value) || "";
}

function getColumnOptions(colId: string): IDisplayConfig | undefined {
  return displayOptions.value?.[colId];
}

const firstColumnId = computed(() => tableColumns.value[0]?.id);
const firstColumnOptions = computed(() =>
  firstColumnId.value ? getColumnOptions(firstColumnId.value) : undefined
);

const hasActiveFilters = computed(() => {
  return filterStates.value.size > 0;
});

function handleFilterRemove(columnId: string) {
  const newMap = new Map(filterStates.value);
  newMap.delete(columnId);
  filterStates.value = newMap;
}

function handleClearAllFilters() {
  filterStates.value = new Map();
}
</script>

<template>
  <DetailPageLayout
    :show-side-nav="showFilters && filterPosition === 'sidebar'"
  >
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>
    <template v-if="showFilters && filterPosition === 'sidebar'" #sidebar>
      <FilterSidebar
        v-if="filterableColumnsComputed.length"
        v-model:filter-states="filterStates"
        v-model:search-terms="searchTerms"
        :columns="filterableColumnsComputed"
        :show-search="showSearch"
      />
    </template>
    <template #main>
      <div
        v-if="showFilters && filterPosition === 'sidebar'"
        class="xl:hidden mb-4"
      >
        <SideModal :full-screen="false">
          <template #button>
            <Button
              type="primary"
              size="small"
              label="Filters"
              icon="filter"
              icon-position="left"
            />
          </template>
          <ContentBlockModal title="Filters">
            <FilterSidebar
              v-if="filterableColumnsComputed.length"
              v-model:filter-states="filterStates"
              v-model:search-terms="searchTerms"
              :columns="filterableColumnsComputed"
              :show-search="showSearch"
              :mobile-display="true"
            />
          </ContentBlockModal>
          <template #footer="{ hide }">
            <Button
              type="secondary"
              size="small"
              label="View results"
              @click="hide()"
            />
          </template>
        </SideModal>
      </div>

      <div
        class="bg-content rounded-t-3px rounded-b-50px shadow-primary overflow-hidden"
      >
        <div
          v-if="showSearch && (!showFilters || filterPosition !== 'sidebar')"
          class="p-5 border-b border-black/10"
        >
          <InputSearch
            :id="searchInputId"
            v-model="searchTerms"
            placeholder="Search..."
            size="small"
          />
        </div>

        <div v-if="hasActiveFilters" class="px-5 py-3 border-b border-black/10">
          <ActiveFilters
            :filters="filterStates"
            :columns="filterableColumnsComputed"
            @remove="handleFilterRemove"
            @clear-all="handleClearAllFilters"
          />
        </div>

        <LoadingContent
          :id="`emx2-data-view-${schemaId}-${tableId}`"
          :status="status"
          loading-text="Loading..."
          :error-text="errorText"
          :show-slot-on-error="false"
        >
          <div v-if="rows.length && layout === 'table'" class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="border-b border-black/10">
                  <th
                    v-for="col in tableColumns"
                    :key="col.id"
                    class="px-3 py-2 text-body-sm font-semibold bg-table"
                  >
                    {{ col.label || col.id }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, index) in rows"
                  :key="index"
                  class="border-b border-black/10 hover:bg-black/5"
                >
                  <td
                    v-for="(col, colIndex) in tableColumns"
                    :key="col.id"
                    class="px-3 py-2 text-body-base bg-table"
                  >
                    <NuxtLink
                      v-if="colIndex === 0 && firstColumnOptions?.getHref"
                      :to="firstColumnOptions.getHref(col, row)"
                      class="text-link hover:underline"
                    >
                      {{ getCellValue(row, col.id) }}
                    </NuxtLink>
                    <span
                      v-else-if="
                        colIndex === 0 && firstColumnOptions?.clickAction
                      "
                      class="text-link hover:underline cursor-pointer"
                      @click="firstColumnOptions.clickAction!(col, row)"
                    >
                      {{ getCellValue(row, col.id) }}
                    </span>
                    <span v-else>{{ getCellValue(row, col.id) }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else-if="rows.length && layout === 'cards'" class="p-5">
            <CardList>
              <CardListItem v-for="(row, index) in rows" :key="index">
                <slot name="card" :row="row" :label="getLabel(row)">
                  <span>{{ getLabel(row) }}</span>
                </slot>
              </CardListItem>
            </CardList>
          </div>

          <div v-else-if="rows.length" class="p-5">
            <ul class="grid gap-1 pl-4 list-disc list-outside">
              <li v-for="(row, index) in rows" :key="index">
                <slot :row="row" :label="getLabel(row)">
                  <span>{{ getLabel(row) }}</span>
                </slot>
              </li>
            </ul>
          </div>

          <div v-else class="p-5">
            <p class="text-body-base opacity-60 italic">No items</p>
          </div>
        </LoadingContent>

        <div v-if="showPagination" class="p-5 border-t border-black/10">
          <InlinePagination
            :current-page="page"
            :total-pages="totalPages"
            @update:page="page = $event"
          />
        </div>
      </div>
    </template>
  </DetailPageLayout>
</template>
