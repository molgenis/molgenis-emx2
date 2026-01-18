<script setup lang="ts">
import { ref, computed, watch, useId } from "vue";
import type {
  IColumn,
  IRow,
  IRefColumn,
} from "../../../../metadata-utils/src/types";
import { useTableData } from "../../composables/useTableData";
import { rowToString } from "../../utils/rowToString";
import InputSearch from "../input/Search.vue";
import InlinePagination from "./InlinePagination.vue";
import LoadingContent from "../LoadingContent.vue";
import ValueRef from "../value/Ref.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    filter?: object;
    viewColumns?: string[];
    showSearch?: boolean;
    pagingLimit?: number;
    refLabel?: string;
    getRefClickAction?: (col: IColumn, row: IRow) => () => void;
  }>(),
  {
    showSearch: true,
    pagingLimit: 10,
  }
);

const searchInputId = useId();
const page = ref(1);
const searchTerms = ref("");

const {
  metadata,
  rows,
  count,
  status,
  totalPages,
  showPagination,
  errorMessage,
} = useTableData(props.schemaId, props.tableId, {
  pageSize: props.pagingLimit,
  page,
  filter: props.filter,
  searchTerms,
});

const errorText = computed(
  () =>
    errorMessage.value ||
    (status.value === "error" ? "Failed to load data" : undefined)
);

// construct a ref column for ValueRef rendering
const refColumn = computed<IRefColumn | undefined>(() => {
  if (!metadata.value) return undefined;
  const keyCol = metadata.value.columns?.find((c) => c.key === 1);
  return {
    id: metadata.value.id,
    label: metadata.value.label,
    columnType: "REF",
    refTableId: metadata.value.id,
    refSchemaId: metadata.value.schemaId,
    refLabel: props.refLabel || keyCol?.refLabel,
    refLabelDefault: keyCol?.refLabelDefault || "${name}",
    refLinkId: keyCol?.id || "name",
  };
});

// reset page on search
watch(searchTerms, () => {
  page.value = 1;
});

function handleRefClick(row: IRow) {
  if (props.getRefClickAction && refColumn.value) {
    props.getRefClickAction(refColumn.value, row)();
  }
}

// for slot: compute label using same logic as ValueRef
function getLabel(row: IRow): string {
  const template =
    refColumn.value?.refLabel || refColumn.value?.refLabelDefault;
  return template ? rowToString(row, template) || "" : "";
}
</script>

<template>
  <div class="emx2-list-view">
    <InputSearch
      v-if="showSearch"
      :id="searchInputId"
      v-model="searchTerms"
      placeholder="Search..."
      size="small"
      class="mb-4"
    />

    <LoadingContent
      :id="`emx2-list-view-${schemaId}-${tableId}`"
      :status="status"
      loading-text="Loading..."
      :error-text="errorText"
      :show-slot-on-error="false"
    >
      <ul v-if="rows.length" class="grid gap-1 pl-4 list-disc list-outside">
        <li v-for="(row, index) in rows" :key="index">
          <!-- default slot with ValueRef as fallback -->
          <slot :row="row" :column="refColumn" :label="getLabel(row)">
            <ValueRef
              v-if="refColumn"
              :metadata="refColumn"
              :data="row"
              @refCellClicked="handleRefClick(row)"
            />
          </slot>
        </li>
      </ul>
      <p v-else class="text-gray-400 dark:text-gray-500 italic">No items</p>

      <InlinePagination
        v-if="showPagination"
        :current-page="page"
        :total-pages="totalPages"
        @update:page="page = $event"
      />
    </LoadingContent>
  </div>
</template>
