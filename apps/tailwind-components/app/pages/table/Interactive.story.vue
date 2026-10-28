<script setup lang="ts">
import { computed, ref } from "vue";
import type {
  columnValue,
  IColumn,
  ITableMetaData,
  TableRow,
} from "../../../../metadata-utils/src/types";
import type {
  ActiveFilter,
  IFilterValue,
  IGraphQLFilter,
  UseFilters,
} from "../../../types/filters";
import type { cellPayload, ITableSettings } from "../../../types/types";
import type { CountedOption } from "../../utils/fetchCounts";
import { buildGraphQLFilter } from "../../utils/buildGqlFilter";
import { formatFilterValue } from "../../utils/formatFilterValue";
import { buildLabelMap } from "../../composables/useFilters";
import TableInteractive from "../../components/table/TableInteractive.vue";
import TableControlColumns from "../../components/table/control/Columns.vue";
import Button from "../../components/Button.vue";
import Checkbox from "../../components/input/Checkbox.vue";

const columns: IColumn[] = [
  { id: "id", label: "Id", columnType: "STRING", key: 1, position: 0 },
  { id: "name", label: "Name", columnType: "STRING", position: 1 },
  { id: "birthDate", label: "Birth date", columnType: "DATE", position: 2 },
  {
    id: "doctor",
    label: "Doctor",
    columnType: "REF",
    refTableId: "Doctor",
    refSchemaId: "hospital-demo",
    refLabelDefault: "${name}",
    position: 3,
  },
  {
    id: "hospital",
    label: "Hospital",
    columnType: "REF",
    refTableId: "Hospital",
    refSchemaId: "hospital-demo",
    refLabelDefault: "${name}",
    position: 4,
  },
  { id: "visits", label: "Visits", columnType: "INT", position: 5 },
  { id: "insured", label: "Insured", columnType: "BOOL", position: 6 },
  {
    id: "notes",
    label: "Notes",
    columnType: "TEXT",
    position: 7,
  },
] as IColumn[];

const tableMetadata: ITableMetaData = {
  id: "Patient",
  schemaId: "hospital-demo",
  name: "Patient",
  label: "Patient",
  tableType: "DATA",
  columns,
} as ITableMetaData;

const hospitals = ["Erasmus MC", "UMCG", "Radboudumc", "Amsterdam UMC"];
const doctors = [
  "Dr. Jansen",
  "Dr. de Vries",
  "Dr. Bakker",
  "Dr. Visser",
  "Dr. Smit",
];
const patientNames = [
  "John Doe",
  "Jane Smith",
  "Alice Johnson",
  "Bob Brown",
  "Charlie Davis",
  "David Evans",
  "Eve Foster",
  "Frank Green",
  "Grace Harris",
  "Hannah Ingram",
  "Ian Johnson",
  "Jack King",
  "Karen Lewis",
  "Liam Moore",
  "Mia Nelson",
  "Noah Owens",
  "Olivia Parker",
  "Peter Quinn",
  "Quinty Reed",
  "Ruben Scott",
  "Sophie Turner",
  "Tom Underwood",
  "Uma Vance",
  "Victor White",
];

const allRows: TableRow[] = patientNames.map((name, index) => {
  const id = `P${String(index + 1).padStart(3, "0")}`;
  return {
    id,
    name,
    birthDate: `19${50 + ((index * 7) % 50)}-${String(
      (index % 12) + 1
    ).padStart(2, "0")}-${String((index % 28) + 1).padStart(2, "0")}`,
    doctor: { name: doctors[index % doctors.length] },
    hospital: { name: hospitals[index % hospitals.length] },
    visits: (index * 3) % 17,
    insured: index % 3 !== 0,
    notes:
      index % 4 === 0
        ? "Follow-up appointment scheduled after reviewing the latest lab results; patient reports mild improvement but still experiences occasional dizziness in the morning."
        : null,
    mg_draft: index % 5 === 0,
    mg_roles: [index % 2 === 0 ? "Owner" : "Viewer"],
    _rowId: { id },
    _rowIdString: id,
  } as TableRow;
});

const features = ref({
  showSearch: true,
  showFilters: true,
  showStickyHeader: true,
  showDraftColumn: true,
  showRolesColumn: false,
});

const permissions = ref({
  canUpdate: true,
  canInsert: true,
  canDelete: true,
  canModifySelection: true,
  isRowLevel: false,
});

const showEmptyTable = ref(false);
const eventLog = ref<string[]>([]);

const settings = ref<ITableSettings>({
  page: 1,
  pageSize: 10,
  orderby: { column: "name", direction: "ASC" },
  search: "",
  orderedVisibleColumnsIds: columns
    .filter((column) => column.id !== "notes")
    .map((column) => column.id),
});

const selectedRows = ref<Map<string, Record<string, columnValue>>>(new Map());

const columnsForColumnControl = computed<IColumn[]>(() => {
  const visibleIds = settings.value.orderedVisibleColumnsIds;
  const visible = visibleIds
    .map((id) => columns.find((column) => column.id === id))
    .filter((column): column is IColumn => !!column);
  const hidden = columns.filter((column) => !visibleIds.includes(column.id));
  return [...visible, ...hidden].map((column, position) => ({
    ...column,
    position,
    visible: visibleIds.includes(column.id) ? "true" : "false",
  }));
});

const sortedVisibleColumns = computed<IColumn[]>(() =>
  settings.value.orderedVisibleColumnsIds
    .map((id) => columns.find((column) => column.id === id))
    .filter((column): column is IColumn => !!column)
);

const filterColumns = columns.filter((column) =>
  ["hospital", "doctor", "insured", "visits"].includes(column.id)
);
const filterStates = ref<Map<string, IFilterValue>>(new Map());
const visibleFilterIds = ref<string[]>(["hospital", "doctor", "insured"]);
const collapsedFilterIds = ref(new Set<string>());
const searchValue = computed(() => settings.value.search ?? "");

function countOptions(columnId: string, names: string[]): CountedOption[] {
  return names.map((name) => ({
    name,
    label: name,
    count: allRows.filter(
      (row) => (row[columnId] as { name: string } | undefined)?.name === name
    ).length,
    overlap: 0,
  }));
}

const countedOptions: Record<string, CountedOption[]> = {
  hospital: countOptions("hospital", hospitals),
  doctor: countOptions("doctor", doctors),
  insured: [
    {
      name: "true",
      count: allRows.filter((row) => row.insured).length,
      overlap: 0,
    },
    {
      name: "false",
      count: allRows.filter((row) => !row.insured).length,
      overlap: 0,
    },
  ],
};

const activeFilters = computed<ActiveFilter[]>(() =>
  [...filterStates.value].flatMap(([columnId, filterValue]) => {
    const column = columns.find((col) => col.id === columnId)!;
    const { displayValue, values } = formatFilterValue(
      filterValue,
      buildLabelMap(column, null)
    );
    return displayValue
      ? [{ columnId, labelParts: [column.label], displayValue, values }]
      : [];
  })
);

const gqlFilter = computed<IGraphQLFilter>(() =>
  buildGraphQLFilter(filterStates.value, columns, searchValue.value)
);

function setFilter(columnId: string, value: IFilterValue | null) {
  const next = new Map(filterStates.value);
  if (value === null) {
    next.delete(columnId);
  } else {
    next.set(columnId, value);
  }
  filterStates.value = next;
  settings.value = { ...settings.value, page: 1 };
}

function setSearch(search: string) {
  settings.value = { ...settings.value, search, page: 1 };
}

function clearFilters() {
  filterStates.value = new Map();
  setSearch("");
}

function toggleFilter(columnId: string) {
  if (visibleFilterIds.value.includes(columnId)) {
    visibleFilterIds.value = visibleFilterIds.value.filter(
      (id) => id !== columnId
    );
    setFilter(columnId, null);
  } else {
    visibleFilterIds.value = [columnId, ...visibleFilterIds.value];
  }
}

const filters: UseFilters = {
  filterStates,
  searchValue,
  gqlFilter,
  activeFilters,
  setFilter,
  setSearch,
  clearFilters,
  removeFilter: (columnId: string) => setFilter(columnId, null),
  columns: ref(filterColumns),
  visibleFilterIds,
  visibleColumns: computed(() =>
    visibleFilterIds.value
      .map((id) => filterColumns.find((column) => column.id === id))
      .filter((column): column is IColumn => !!column)
  ),
  toggleFilter,
  resetFilters: () => {
    clearFilters();
    visibleFilterIds.value = ["hospital", "doctor", "insured"];
  },
  getCountedOptions: (columnId: string) =>
    computed(() => countedOptions[columnId] ?? []),
  isCountLoading: () => computed(() => false),
  isSaturated: () => computed(() => false),
  hasCountError: () => computed(() => false),
  nestedColumnMeta: ref(new Map()),
  registerNestedColumn: () => {},
  schemaId: ref(tableMetadata.schemaId),
  tableId: ref(tableMetadata.id),
  toggleCollapse: (columnId: string) => {
    const next = new Set(collapsedFilterIds.value);
    if (next.has(columnId)) {
      next.delete(columnId);
    } else {
      next.add(columnId);
    }
    collapsedFilterIds.value = next;
  },
  isCollapsed: (columnId: string) => collapsedFilterIds.value.has(columnId),
};

function displayValue(value: unknown): string {
  if (value && typeof value === "object" && "name" in value) {
    return String((value as { name: string }).name);
  }
  return value === null || value === undefined ? "" : String(value);
}

function matchesFilter(row: TableRow, columnId: string, filter: IFilterValue) {
  const value = row[columnId];
  if (filter.operator === "between") {
    const [min, max] = filter.value as [number | null, number | null];
    const numeric = Number(value);
    return (min == null || numeric >= min) && (max == null || numeric <= max);
  }
  if (filter.operator === "isNull") {
    return value === null || value === undefined;
  }
  if (filter.operator === "notNull") {
    return value !== null && value !== undefined;
  }
  const expected = Array.isArray(filter.value)
    ? filter.value.map(String)
    : [String(filter.value)];
  return expected.includes(displayValue(value));
}

const filteredRows = computed(() => {
  const search = (settings.value.search ?? "").toLowerCase();
  return allRows
    .filter((row) =>
      [...filterStates.value].every(([columnId, filter]) =>
        matchesFilter(row, columnId, filter)
      )
    )
    .filter((row) =>
      columns.some((column) =>
        displayValue(row[column.id]).toLowerCase().includes(search)
      )
    );
});

const sortedRows = computed(() => {
  const { column, direction } = settings.value.orderby;
  const factor = direction === "ASC" ? 1 : -1;
  return [...filteredRows.value].sort((a, b) => {
    const left = a[column];
    const right = b[column];
    if (typeof left === "number" && typeof right === "number") {
      return (left - right) * factor;
    }
    return displayValue(left).localeCompare(displayValue(right)) * factor;
  });
});

const pageRows = computed(() => {
  if (showEmptyTable.value) {
    return [];
  }
  const start = (settings.value.page - 1) * settings.value.pageSize;
  return sortedRows.value.slice(start, start + settings.value.pageSize);
});

const rowCount = computed(() =>
  showEmptyTable.value ? 0 : sortedRows.value.length
);

function log(message: string) {
  eventLog.value = [message, ...eventLog.value].slice(0, 10);
}

function handleUpdateSettings(value: ITableSettings) {
  settings.value = value;
  log(`update:settings ${JSON.stringify(value)}`);
}

function handleColumnsUpdate(visibleColumns: IColumn[]) {
  settings.value = {
    ...settings.value,
    orderedVisibleColumnsIds: visibleColumns.map((column) => column.id),
  };
}

function toggleRowSelection(row: TableRow) {
  const next = new Map(selectedRows.value);
  if (next.has(row._rowIdString)) {
    next.delete(row._rowIdString);
  } else {
    next.set(row._rowIdString, row._rowId);
  }
  selectedRows.value = next;
}

function selectRows(rows: TableRow[]) {
  selectedRows.value = new Map(
    rows.map((row) => [row._rowIdString, row._rowId])
  );
}

function handleRowAction(payload: { action: string }) {
  log(`rowAction ${payload.action}`);
  switch (payload.action) {
    case "select-all-on-page":
      selectRows(pageRows.value);
      break;
    case "select-none":
      selectRows([]);
      break;
    case "select-drafts":
      selectRows(pageRows.value.filter((row) => row.mg_draft === true));
      break;
    default:
      log(`selected: ${[...selectedRows.value.keys()].join(", ") || "none"}`);
  }
}

function handleCellClicked(payload: cellPayload) {
  log(`cellClicked ${payload.metadata.id}: ${JSON.stringify(payload.data)}`);
}

const featureOptions = [
  { key: "showSearch", label: "Show search" },
  { key: "showFilters", label: "Show filters" },
  { key: "showStickyHeader", label: "Sticky header (reload story)" },
  { key: "showDraftColumn", label: "Show draft column" },
  { key: "showRolesColumn", label: "Show roles column" },
] as const;

const permissionOptions = [
  { key: "canInsert", label: "Can insert" },
  { key: "canUpdate", label: "Can update" },
  { key: "canDelete", label: "Can delete" },
  { key: "canModifySelection", label: "Can modify selection" },
  { key: "isRowLevel", label: "Is row level" },
] as const;
</script>

<template>
  <div class="py-5">
    <div class="flex flex-wrap gap-4 mb-8">
      <fieldset class="border border-form rounded-theme p-4">
        <legend class="text-title">Features</legend>
        <div
          v-for="option in featureOptions"
          :key="option.key"
          class="flex items-center gap-2"
        >
          <Checkbox
            :id="`feature-${option.key}`"
            v-model="features[option.key]"
          />
          <label :for="`feature-${option.key}`" class="text-title">
            {{ option.label }}
          </label>
        </div>
      </fieldset>

      <fieldset class="border border-form rounded-theme p-4">
        <legend class="text-title">Permissions</legend>
        <div
          v-for="option in permissionOptions"
          :key="option.key"
          class="flex items-center gap-2"
        >
          <Checkbox
            :id="`permission-${option.key}`"
            v-model="permissions[option.key]"
          />
          <label :for="`permission-${option.key}`" class="text-title">
            {{ option.label }}
          </label>
        </div>
      </fieldset>

      <fieldset class="border border-form rounded-theme p-4">
        <legend class="text-title">Data</legend>
        <div class="flex items-center gap-2">
          <Checkbox id="show-empty-table" v-model="showEmptyTable" />
          <label for="show-empty-table" class="text-title">
            Show empty table
          </label>
        </div>
      </fieldset>

      <fieldset class="border border-form rounded-theme p-4 flex-1 min-w-80">
        <legend class="text-title">Emitted events</legend>
        <ul class="text-body-sm text-title font-mono max-h-40 overflow-auto">
          <li v-for="(entry, index) in eventLog" :key="index" class="truncate">
            {{ entry }}
          </li>
        </ul>
      </fieldset>
    </div>

    <TableInteractive
      :features="features"
      :filters="filters"
      :permissions="permissions"
      :rows="pageRows"
      :rowCount="rowCount"
      :settings="settings"
      :selectedRows="selectedRows"
      :sortedVisibleColumns="sortedVisibleColumns"
      :tableMetadata="tableMetadata"
      searchPlaceholder="Search patients"
      @update:settings="handleUpdateSettings"
      @toggleRowSelection="toggleRowSelection"
      @rowAction="handleRowAction"
      @cellClicked="handleCellClicked"
    >
      <template #buttons>
        <div class="flex gap-[10px]">
          <Button
            v-if="permissions.canInsert"
            type="primary"
            size="medium"
            icon="add-circle"
            @click="log('add patient clicked')"
          >
            Add Patient
          </Button>
          <TableControlColumns
            :columns="columnsForColumnControl"
            @update:columns="handleColumnsUpdate"
          />
        </div>
      </template>

      <template #row-action="{ row }">
        <Button
          v-if="permissions.canDelete"
          :icon-only="true"
          type="inline"
          icon="trash"
          label="delete"
          @click="log(`delete ${row._rowIdString}`)"
        />
        <Button
          v-if="permissions.canUpdate"
          :icon-only="true"
          type="inline"
          icon="edit"
          label="edit"
          @click="log(`edit ${row._rowIdString}`)"
        />
        <Button
          v-if="permissions.canInsert"
          :icon-only="true"
          type="inline"
          icon="copy"
          label="copy"
          @click="log(`copy ${row._rowIdString}`)"
        />
      </template>
    </TableInteractive>
  </div>
</template>
