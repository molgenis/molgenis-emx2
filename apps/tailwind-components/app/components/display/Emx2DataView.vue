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
import Pagination from "../Pagination.vue";
import LoadingContent from "../LoadingContent.vue";
import CardList from "../CardList.vue";
import CardListItem from "../CardListItem.vue";
import FilterSidebar from "../filter/Sidebar.vue";
import ActiveFilters from "../filter/ActiveFilters.vue";
import DetailPageLayout from "../layout/DetailPageLayout.vue";
import SideModal from "../SideModal.vue";
import ContentBlockModal from "../content/ContentBlockModal.vue";
import Button from "../Button.vue";
import Columns from "../table/control/Columns.vue";
import RecordCard from "./RecordCard.vue";
import EditModal from "../form/EditModal.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    config?: IDisplayConfig;
    urlSync?: boolean;
    isEditable?: boolean;
  }>(),
  {
    urlSync: true,
    isEditable: false,
  }
);

defineSlots<{
  header?: () => any;
  default?: (props: { row: IRow; label: string }) => any;
  card?: (props: { row: IRow; label: string }) => any;
}>();

const layout = computed(() => props.config?.layout || "table");
const showFilters = computed(() => props.config?.showFilters || false);
const filtersVisible = ref(props.config?.showFilters || false);
const filterPosition = computed(
  () => props.config?.filterPosition || "sidebar"
);
const showSearch = computed(() => props.config?.showSearch !== false);
const pagingLimit = computed(() => props.config?.pageSize || 10);
const rowLabel = computed(() => props.config?.rowLabel);
const visibleColumns = computed(() => props.config?.visibleColumns);
const displayOptions = computed(() => props.config?.columnConfig);

const searchInputId = useId();
const page = ref(1);
const searchTerms = ref("");
const showAddModal = ref(false);

const route = useRoute();
const router = useRouter();

const metadataRef = ref<IColumn[]>([]);

const { filterStates, gqlFilter } = useFilters(metadataRef, {
  debounceMs: 300,
  urlSync: props.urlSync,
  route: route as any,
  router: router as any,
});

const { metadata, rows, status, totalPages, showPagination, errorMessage, refresh } =
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

function handleColumnsUpdate(updatedColumns: IColumn[]) {
  metadataRef.value = updatedColumns;
}

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
  if (!metadataRef.value.length) return [];
  if (visibleColumns.value && visibleColumns.value.length > 0) {
    return visibleColumns.value.filter((colId) =>
      metadataRef.value.some((c) => c.id === colId && c.visible !== "false")
    );
  }
  return metadataRef.value
    .filter((c) => !c.id.startsWith("mg_") && c.visible !== "false")
    .map((c) => c.id);
});

const tableColumns = computed<IColumn[]>(() => {
  if (!metadataRef.value.length) return [];
  return visibleColumnsComputed.value
    .map((colId) => metadataRef.value.find((c) => c.id === colId))
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

async function afterClose() {
  await refresh();
}
</script>

<template>
  <DetailPageLayout
    :show-side-nav="filtersVisible && filterPosition === 'sidebar'"
  >
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>
    <template v-if="filtersVisible && filterPosition === 'sidebar'" #sidebar>
      <FilterSidebar
        v-if="metadataRef.length"
        v-model:filter-states="filterStates"
        v-model:search-terms="searchTerms"
        :all-columns="metadataRef"
        :show-search="showSearch"
        @update:columns="handleColumnsUpdate"
      />
    </template>
    <template #main>
      <div class="xl:hidden mb-4 flex gap-2">
        <Button
          v-if="isEditable"
          type="primary"
          size="small"
          icon="add-circle"
          @click="showAddModal = true"
        >
          Add {{ tableId }}
        </Button>
        <SideModal v-if="filtersVisible" :full-screen="false">
          <template #button>
            <Button type="outline" size="small" icon="filter">
              Filters
            </Button>
          </template>
          <ContentBlockModal title="Filters">
            <FilterSidebar
              v-if="metadataRef.length"
              v-model:filter-states="filterStates"
              v-model:search-terms="searchTerms"
              :all-columns="metadataRef"
              :show-search="showSearch"
              :mobile-display="true"
              @update:columns="handleColumnsUpdate"
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
        <Columns
          v-if="metadataRef.length"
          :columns="metadataRef"
          size="small"
          @update:columns="handleColumnsUpdate"
        />
      </div>

      <div class="bg-content rounded-t-3px shadow-primary">
        <div
          v-if="metadata?.columns"
          class="hidden xl:flex p-5 border-b border-black/10 justify-between"
        >
          <div class="flex gap-2">
            <Button
              v-if="isEditable"
              type="primary"
              icon="add-circle"
              @click="showAddModal = true"
            >
              Add {{ tableId }}
            </Button>
          </div>
          <div class="flex gap-2">
            <Button
              v-if="showFilters && filterPosition === 'sidebar'"
              type="outline"
              @click="filtersVisible = !filtersVisible"
            >
              {{ filtersVisible ? 'Hide Filters' : 'Show Filters' }}
            </Button>
            <Columns
              :columns="metadataRef"
              @update:columns="handleColumnsUpdate"
            />
          </div>
        </div>

        <div
          v-if="showSearch && (!showFilters || filterPosition !== 'sidebar')"
          class="p-3 xl:p-5 border-b border-black/10"
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
            :columns="metadataRef"
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
          <div v-if="rows.length && layout === 'table'">
            <div class="md:hidden p-3 space-y-3">
              <RecordCard
                v-for="(row, index) in rows"
                :key="index"
                :row="row"
                :columns="tableColumns"
              />
            </div>
            <div class="hidden md:block overflow-x-auto">
              <table class="min-w-full text-left border-collapse">
                <thead>
                  <tr class="border-b border-black/10">
                    <th
                      v-for="(col, colIndex) in tableColumns"
                      :key="col.id"
                      :class="[
                        'px-3 py-2 text-body-sm font-semibold bg-table text-table-column-header whitespace-nowrap',
                        colIndex === 0
                          ? 'sticky left-0 z-10 shadow-[2px_0_4px_-2px_rgba(0,0,0,0.1)]'
                          : '',
                      ]"
                    >
                      {{ col.label || col.id }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(row, index) in rows"
                    :key="index"
                    class="border-b border-black/10 hover:bg-black/5 group"
                  >
                    <td
                      v-for="(col, colIndex) in tableColumns"
                      :key="col.id"
                      :class="[
                        'px-3 py-2 text-body-base bg-table text-table-row whitespace-nowrap',
                        colIndex === 0
                          ? 'sticky left-0 z-10 shadow-[2px_0_4px_-2px_rgba(0,0,0,0.1)] group-hover:bg-black/5'
                          : '',
                      ]"
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
          </div>

          <div v-else-if="rows.length && layout === 'cards'" class="p-5 pb-8">
            <CardList>
              <CardListItem v-for="(row, index) in rows" :key="index">
                <slot name="card" :row="row" :label="getLabel(row)">
                  <span>{{ getLabel(row) }}</span>
                </slot>
              </CardListItem>
            </CardList>
          </div>

          <div v-else-if="rows.length" class="p-5 pb-8">
            <ul class="grid gap-1 pl-4 list-disc list-outside">
              <li v-for="(row, index) in rows" :key="index">
                <slot :row="row" :label="getLabel(row)">
                  <span>{{ getLabel(row) }}</span>
                </slot>
              </li>
            </ul>
          </div>

          <div v-else class="p-5 pb-8">
            <p class="text-body-base opacity-60 italic">No items</p>
          </div>
        </LoadingContent>
      </div>
      <div class="bg-content rounded-b-50px h-8 -mt-1 shadow-primary"></div>

      <div v-if="showPagination" class="pb-12.5">
        <Pagination
          :current-page="page"
          :total-pages="totalPages"
          :prevent-default="true"
          @update="page = $event"
        />
      </div>
    </template>
  </DetailPageLayout>
  <EditModal
    v-if="metadata && showAddModal"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="metadata"
    :isInsert="true"
    v-model:visible="showAddModal"
    @update:cancelled="afterClose"
  />
</template>
