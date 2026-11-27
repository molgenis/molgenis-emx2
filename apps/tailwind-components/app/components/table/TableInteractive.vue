<script setup lang="ts">
import {
  useDebounceFn,
  useElementBounding,
  useElementSize,
} from "@vueuse/core";
import { computed, nextTick, onMounted, onUnmounted, ref } from "vue";
import type {
  columnValue,
  IColumn,
  ITableMetaData,
  TableRow,
} from "../../../../metadata-utils/src/types.ts";
import type { UseFilters } from "../../../types/filters.ts";
import type { cellPayload, ITableSettings } from "../../../types/types.ts";
import { useColumnResize } from "../../composables/useColumnResize";
import { FILTER_DEBOUNCE } from "../../composables/useFilters";
import { getCountMessage } from "../../utils/getCountMessage";
import ActiveFilters from "../filter/ActiveFilters.vue";
import FilterSidebarContent from "../filter/SidebarContent.vue";
import Checkbox from "../input/Checkbox.vue";
import InputSearch from "../input/Search.vue";
import DraftLabel from "../label/DraftLabel.vue";
import Pagination from "../Pagination.vue";
import Sidebar from "../Sidebar.vue";
import Button from "../Button.vue";
import TableCellEMX2 from "./CellEMX2.vue";
import ActionMenuItem from "./control/ActionMenuItem.vue";
import RowControls from "./control/RowControls.vue";

const STICKY_SELECT_COLUMN_WIDTH = 48;
// Toolbar indent before it can be measured (SSR, first paint): the expanded
// sidebar width (w-80 / xl:w-96) plus gap-6, minus the lg:-ml-[30px] offset.
const SIDEBAR_INSET_ESTIMATE =
  "@5xl/toolbar:pl-[344px] lg:@5xl/toolbar:pl-[314px] xl:@5xl/toolbar:pl-[378px]";

const props = withDefaults(
  defineProps<{
    features: FeatureFlags;
    filters?: UseFilters | null;
    permissions: Permissions;
    rows: TableRow[];
    rowCount: number;
    settings: ITableSettings;
    searchPlaceholder?: string;
    selectedRows?: Map<string, Record<string, columnValue>>;
    sortedVisibleColumns?: IColumn[];
    tableMetadata: ITableMetaData;
  }>(),
  {
    searchPlaceholder: "Search",
  }
);

const emit = defineEmits<{
  (event: "update:settings", value: ITableSettings): void;
  (event: "rowAction", payload: { action: string }): void;
  (event: "toggleRowSelection", row: TableRow): void;
  (event: "cellClicked", payload: cellPayload): void;
}>();

const selectColumnStyle = {
  width: `${STICKY_SELECT_COLUMN_WIDTH}px`,
};

const sidebarCollapsed = ref(false);
const showStickyHeader = ref(false);
const tableContainer = ref<HTMLElement | null>(null);
const tableHeaderFixed = ref<HTMLElement | null>(null);
const tableHead = ref<HTMLElement | null>(null);
const { columnWidths, guideX, startResize, setInitialWidths, isResizing } =
  useColumnResize(tableContainer);

const columns = computed(
  () => props.sortedVisibleColumns || props.tableMetadata.columns
);
const numberOfSelectedRows = computed(() => props.selectedRows?.size || 0);

// The layout switch itself is CSS (container queries), so SSR renders it
// correctly; this width is only needed for elements outside the toolbar
// container that cannot use its container queries: the teleported "more" menu
// and the filter sidebar (a drawer over the table while the toolbar shows a
// filters button, hidden when collapsed).
// It is read from a probe element that the same `@5xl/toolbar` container
// variant shows, so it always agrees with the CSS (whatever the root font size).
const toolbar = ref<HTMLElement | null>(null);
const wideToolbarProbe = ref<HTMLElement | null>(null);
const { width: toolbarWidth } = useElementSize(toolbar);
const { width: wideToolbarProbeWidth } = useElementSize(wideToolbarProbe);
const isCompact = computed(
  () => toolbarWidth.value > 0 && wideToolbarProbeWidth.value === 0
);

// Indent of the toolbar content so it lines up with the table's left edge
// (next to the pushing sidebar on wide toolbars); null until measured.
const tableColumn = ref<HTMLElement | null>(null);
const { left: toolbarLeft } = useElementBounding(toolbar);
const { left: tableColumnLeft, width: tableColumnWidth } =
  useElementBounding(tableColumn);
const tableInset = computed<number | null>(() => {
  if (!props.features.showFilters) return 0;
  if (!toolbarWidth.value || !tableColumnWidth.value) return null;
  if (isCompact.value) return 0;
  return Math.max(0, Math.round(tableColumnLeft.value - toolbarLeft.value));
});
const searchExpanded = ref(!!props.settings.search);

async function expandSearch() {
  searchExpanded.value = true;
  await nextTick();
  document.getElementById("search-input")?.focus();
}

function runMenuAction(hide: () => void, action: string) {
  hide();
  handleRowAction({ action });
}

const hasFiltersOrSearch = computed(
  () =>
    (props.filters?.activeFilters.value.length ?? 0) ||
    (props.filters?.searchValue.value ?? "").length
);
const emptyRowsLabel = computed(() =>
  hasFiltersOrSearch.value ? "No data matched the filters" : "No records found"
);
const handleSearchChange = useDebounceFn((search?: string) => {
  emit("update:settings", { ...props.settings, search, page: 1 });
}, FILTER_DEBOUNCE);
const countMessage = computed(() =>
  getCountMessage(props.settings.page, props.settings.pageSize, props.rowCount)
);
const totalPages = computed(() =>
  Math.max(1, Math.ceil(props.rowCount / props.settings.pageSize))
);

onMounted(async () => {
  setInitialWidths(columns.value);
  sidebarCollapsed.value = window.matchMedia("(max-width: 1023px)").matches;
  if (props.features.showStickyHeader) {
    window.addEventListener("resize", updateStickyHeaderWidth);
    window.addEventListener("scroll", handleStickyHeaderScroll);
  }
});

onUnmounted(async () => {
  window.removeEventListener("scroll", handleStickyHeaderScroll);
  window.removeEventListener("resize", updateStickyHeaderWidth);
});

function handleRowAction(payload: { action: string }) {
  emit("rowAction", payload);
}

function handleStickyHeaderScroll(event: Event) {
  const rect = tableContainer?.value?.getBoundingClientRect();
  const top = rect?.top ?? 0;
  showStickyHeader.value = top <= 0;
  updateStickyHeaderWidth();
  const tableHeadHeight = tableHead.value?.getBoundingClientRect().height ?? 0;
  if (rect?.bottom && rect?.bottom <= tableHeadHeight) {
    showStickyHeader.value = false;
  }
}

function handleStickyHeaderOffset(event: Event) {
  const target = event.target as HTMLElement;
  const { scrollLeft } = target;
  if (tableHeaderFixed.value) {
    tableHeaderFixed.value.style.transform = `translateX(-${scrollLeft}px)`;
  }
  updateStickyHeaderWidth();
}

function updateStickyHeaderWidth() {
  const tableFixedContainer = tableHeaderFixed.value?.parentElement;
  if (tableFixedContainer) {
    tableFixedContainer.style.width =
      tableFixedContainer.parentElement?.clientWidth + "px";
  }
}

function handleSortRequest(columnId: string) {
  const isSameColumn = props.settings.orderby.column === columnId;
  const newDirection =
    isSameColumn && props.settings.orderby.direction === "ASC" ? "DESC" : "ASC";

  emit("update:settings", {
    ...props.settings,
    orderby: { column: columnId, direction: newDirection },
  });
}

function toggleRowSelection(row: TableRow) {
  emit("toggleRowSelection", row);
}

function handleCellClick(payload: cellPayload) {
  emit("cellClicked", payload);
}

function handlePagingRequest(page: number) {
  emit("update:settings", { ...props.settings, page });
}

function handlePageSizeChange(pageSize: string) {
  emit("update:settings", {
    ...props.settings,
    pageSize: Number.parseInt(pageSize),
    page: 1,
  });
}

interface FeatureFlags {
  showSearch?: boolean;
  showFilters?: boolean;
  showStickyHeader?: boolean;
  showDraftColumn?: boolean;
  showRolesColumn?: boolean;
  /** the "more" menu has items (from the more-actions slot) on wide toolbars too */
  hasMoreActions?: boolean;
}

interface Permissions {
  canUpdate: boolean;
  canInsert: boolean;
  canDelete: boolean;
  canModifySelection: boolean;
  isRowLevel: boolean;
}
</script>

<template>
  <!--
    Always a single row. Two nested containers:
    - "toolbar" (full width) picks the layout, so it does not depend on the
      sidebar state:
      - below @5xl (compact): [filters] [search button] ... [buttons] [more];
        row selection and secondary actions live in the "more" menu, and the
        search button expands the input over the full row
      - @5xl and up: [row controls] [search input] ... [buttons] [more]
    - the inner (unnamed) container is indented to line up with the table's
      left edge (next to the filter sidebar); from @6xl of its width, buttons
      with responsiveLabel also show their label
  -->
  <div ref="toolbar" class="@container/toolbar mb-[30px]">
    <span
      ref="wideToolbarProbe"
      aria-hidden="true"
      class="hidden @5xl/toolbar:block h-0"
    />
    <div
      class="@container"
      :class="{ [SIDEBAR_INSET_ESTIMATE]: tableInset === null }"
      :style="
        tableInset === null ? undefined : { paddingLeft: `${tableInset}px` }
      "
    >
      <div class="flex flex-nowrap items-center gap-[10px]">
        <Button
          v-if="features.showFilters && filters"
          class="shrink-0 @5xl/toolbar:hidden"
          :class="{ hidden: searchExpanded }"
          type="outline"
          icon="filter-alt"
          :icon-only="true"
          :label="sidebarCollapsed ? 'Show filters' : 'Hide filters'"
          aria-controls="filter-sidebar-content"
          :aria-expanded="!sidebarCollapsed"
          @click="sidebarCollapsed = !sidebarCollapsed"
        />

        <Button
          v-if="features.showSearch"
          class="shrink-0 @5xl/toolbar:hidden"
          :class="{ hidden: searchExpanded }"
          type="outline"
          icon="search"
          :icon-only="true"
          label="Search"
          aria-controls="search-input"
          @click="expandSearch"
        />

        <RowControls
          class="hidden @5xl/toolbar:inline-flex shrink-0 h-50px"
          :numberOfSelectedRows="numberOfSelectedRows"
          :allRowsSelected="
            numberOfSelectedRows === Math.min(settings.pageSize, rows.length)
          "
          :canUpdate="permissions.canUpdate"
          :canDelete="permissions.canDelete"
          :canModifySelection="permissions.canModifySelection"
          @rowAction="handleRowAction"
        />

        <InputSearch
          v-if="features.showSearch"
          class="min-w-0 @5xl/toolbar:flex @5xl/toolbar:basis-48 @5xl/toolbar:grow @5xl/toolbar:max-w-md"
          :class="searchExpanded ? 'grow' : 'hidden'"
          id="search-input"
          size="medium"
          :modelValue="settings.search"
          :placeholder="searchPlaceholder"
          @update:modelValue="handleSearchChange($event)"
        />
        <Button
          v-if="features.showSearch && searchExpanded"
          class="shrink-0 @5xl/toolbar:hidden"
          type="inline"
          icon="cross"
          :icon-only="true"
          label="Close search"
          @click="searchExpanded = false"
        />

        <div
          class="flex shrink-0 items-center gap-[10px] ml-auto"
          :class="{ 'hidden @5xl/toolbar:flex': searchExpanded }"
        >
          <slot name="buttons" :isCompact="isCompact" />

          <VDropdown
            :class="{ '@5xl/toolbar:hidden': !features.hasMoreActions }"
            :distance="6"
            placement="bottom-end"
            :triggers="['click']"
          >
            <Button
              id="table-more-actions"
              type="outline"
              size="medium"
              icon="more-vert"
              label="More actions"
              :icon-only="true"
            />
            <template #popper="{ hide }">
              <ul class="bg-form p-[10px] min-w-[200px] border border-form">
                <template v-if="isCompact">
                  <ActionMenuItem
                    icon="checklist"
                    label="Select all on this page"
                    @click="runMenuAction(hide, 'select-all-on-page')"
                  />
                  <ActionMenuItem
                    icon="cross"
                    label="Select none"
                    :disabled="numberOfSelectedRows === 0"
                    @click="runMenuAction(hide, 'select-none')"
                  />
                  <ActionMenuItem
                    icon="docs"
                    label="Select drafts"
                    @click="runMenuAction(hide, 'select-drafts')"
                  />
                  <ActionMenuItem
                    v-if="permissions.canDelete"
                    icon="trash"
                    label="Delete selection"
                    :disabled="
                      numberOfSelectedRows === 0 ||
                      !permissions.canModifySelection
                    "
                    @click="runMenuAction(hide, 'delete-selection')"
                  />
                  <ActionMenuItem
                    v-if="permissions.canUpdate"
                    icon="edit"
                    label="Edit selection"
                    :disabled="
                      numberOfSelectedRows !== 1 ||
                      !permissions.canModifySelection
                    "
                    @click="runMenuAction(hide, 'edit-selection')"
                  />
                  <ActionMenuItem
                    icon="info"
                    label="View details"
                    :disabled="numberOfSelectedRows !== 1"
                    @click="runMenuAction(hide, 'view-details')"
                  />
                  <li
                    aria-hidden="true"
                    class="my-[5px] border-t border-form"
                  />
                </template>
                <slot name="more-actions" :hide="hide" :isCompact="isCompact" />
              </ul>
            </template>
          </VDropdown>
        </div>
      </div>
    </div>
  </div>

  <!-- on lg the pushing sidebar sits in the page gutter; the drawer does not -->
  <div
    :class="{
      'relative flex gap-6': features.showFilters,
      'overflow-hidden lg:-ml-[30px]': features.showFilters && !isCompact,
    }"
  >
    <!--
      Wide toolbar: the sidebar is a flex item that pushes the table aside.
      Compact toolbar (filters button shown): it is a drawer over the table,
      hidden entirely while collapsed.
    -->
    <div
      v-if="features.showFilters && filters"
      :class="
        isCompact
          ? [
              'absolute top-0 left-0 z-[45] max-w-full rounded-t-base rounded-b-alt shadow-primary',
              { hidden: sidebarCollapsed },
            ]
          : 'contents'
      "
    >
      <Sidebar
        class="max-w-full"
        :collapsed="sidebarCollapsed"
        :animated="!isCompact"
        :activeFilterCount="filters.activeFilters.value.length"
        @update:collapsed="sidebarCollapsed = $event"
      >
        <FilterSidebarContent
          :filters="filters"
          :columns="filters.columns.value"
          :schemaId="tableMetadata.schemaId"
          :tableId="tableMetadata.id"
        />
      </Sidebar>
    </div>

    <div ref="tableColumn" class="flex-1 min-w-0">
      <ActiveFilters
        v-if="features.showFilters && filters"
        :filters="filters.activeFilters.value"
        :searchValue="filters.searchValue.value"
        @remove="filters.removeFilter"
        @clearAll="filters.clearFilters"
        @clearSearch="filters.setSearch('')"
      />
      <slot v-else name="active-filters" />

      <div
        ref="tableContainer"
        class="relative overflow-auto overflow-y-hidden rounded-t-base rounded-b-alt border border-theme border-color-theme"
      >
        <div
          v-if="guideX !== null"
          class="absolute top-0 bottom-0 w-[2px] bg-button-primary pointer-events-none z-50"
          :style="{ left: guideX + 'px' }"
        />

        <ScrollAreaHorizontal
          class="bg-table rounded-t-base"
          :startInset="STICKY_SELECT_COLUMN_WIDTH"
          @scroll="handleStickyHeaderOffset"
        >
          <div
            v-if="features.showStickyHeader"
            class="fixed top-0 z-[36] overflow-hidden aria-hidden=true"
            :class="{ hidden: !showStickyHeader }"
          >
            <table
              ref="tableHeaderFixed"
              class="border-0 text-left w-full table-fixed bg-table"
            >
              <TableEMX2Head
                :schemaId="tableMetadata.schemaId"
                :tableId="tableMetadata.id"
                :settings="settings"
                :columns="columns"
                :showDraftColumn="features.showDraftColumn"
                :showRolesColumn="features.showRolesColumn"
                :isResizing="isResizing"
                :columnWidths="columnWidths"
                :selectColumnWidth="STICKY_SELECT_COLUMN_WIDTH"
                @sortRequested="handleSortRequest"
                @startResize="startResize($event.event, $event.id)"
              />
            </table>
          </div>
          <table ref="table" class="text-left w-full table-fixed">
            <TableEMX2Head
              :schemaId="tableMetadata.schemaId"
              :tableId="tableMetadata.id"
              :settings="settings"
              :columns="columns"
              :showDraftColumn="features.showDraftColumn"
              :showRolesColumn="features.showRolesColumn"
              :isResizing="isResizing"
              :columnWidths="columnWidths"
              :selectColumnWidth="STICKY_SELECT_COLUMN_WIDTH"
              @sortRequested="handleSortRequest"
              @startResize="startResize($event.event, $event.id)"
            />
            <tbody class="mb-3 [&_tr:last-child_td]:after:hidden">
              <tr
                v-if="rows"
                v-for="row in rows"
                class="group h-13"
                :class="{
                  'hover:cursor-pointer':
                    permissions.canInsert ||
                    permissions.canUpdate ||
                    permissions.canDelete,
                }"
              >
                <td
                  class="sticky left-0 z-20 group-hover:z-40 w-12 h-13 py-0 px-2.5 align-middle after:absolute after:inset-x-0 after:bottom-0 after:border-b after:pointer-events-none shadow-[inset_-1px_0_0_var(--border-color-theme)] bg-table group-hover:bg-hover"
                  :style="selectColumnStyle"
                >
                  <Checkbox
                    class="block mx-auto"
                    :modelValue="selectedRows?.has(row._rowIdString)"
                    @update:modelValue="toggleRowSelection(row)"
                  />
                  <div
                    class="absolute left-full top-0 bottom-px flex flex-row text-table-row bg-inherit group-hover:bg-hover invisible group-hover:visible border-none items-center justify-start flex-nowrap gap-1"
                  >
                    <slot name="row-action" :row="row" />
                  </div>
                </td>

                <TableCellEMX2
                  v-if="features.showRolesColumn"
                  class="text-table-row group-hover:bg-hover w-48 h-13 py-0 pl-2 pr-2.5"
                >
                  {{ row.mg_roles?.[0] ?? "" }}
                </TableCellEMX2>

                <TableCellEMX2
                  v-if="features.showDraftColumn"
                  class="text-table-row group-hover:bg-hover h-13 py-0 pl-2 pr-2.5"
                >
                  <DraftLabel v-if="row?.mg_draft === true" type="inline" />
                </TableCellEMX2>

                <TableCellEMX2
                  v-for="column in columns"
                  :style="{ width: columnWidths[column.id] + 'px' }"
                  class="text-table-row group-hover:bg-hover h-13 py-0 pl-2 pr-2.5"
                  :class="{
                    'w-60 lg:w-full': columns.length <= 5,
                    'w-60': columns.length > 5,
                  }"
                  :scope="column.key === 1 ? 'row' : null"
                  :metadata="column"
                  :data="row[column.id]"
                  @cellClicked="handleCellClick"
                />

                <!-- Matches the filler column in the header -->
                <td
                  aria-hidden="true"
                  class="relative after:absolute after:inset-x-0 after:bottom-0 after:border-b after:pointer-events-none after:z-[35] group-hover:bg-hover"
                />
              </tr>
            </tbody>
          </table>
          <div
            class="sticky left-0 flex justify-center items-center py-2.5"
            v-if="!rows?.length"
          >
            <TextNoResultsMessage
              class="w-full text-center"
              :label="emptyRowsLabel"
            />
          </div>
        </ScrollAreaHorizontal>
      </div>

      <div
        class="p-2.5 text-right font-normal align-middle text-table-column-header"
      >
        {{ countMessage }}
      </div>

      <Pagination
        class="pt-0 pb-[30px]"
        :currentPage="settings.page"
        :totalPages="totalPages"
        :jumpToEdge="true"
        :pageSize="settings.pageSize"
        :showPageSizeSelector="true"
        @update="handlePagingRequest($event)"
        @update:pageSize="handlePageSizeChange($event)"
      />
    </div>
  </div>
</template>
