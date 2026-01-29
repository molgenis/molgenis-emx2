<script setup lang="ts">
import { ref, computed } from "vue";
import type {
  IColumn,
  IRow,
  IRefColumn,
} from "../../../../metadata-utils/src/types";
import type { RefPayload } from "../../../types/types";
import { rowToString } from "../../utils/rowToString";
import ValueList from "./List.vue";
import ValueString from "./String.vue";
import ValueText from "./Text.vue";
import ValueDecimal from "./Decimal.vue";
import ValueLong from "./Long.vue";
import ValueInt from "./Int.vue";
import ValueBool from "./Bool.vue";
import ValueEmail from "./Email.vue";
import ValueHyperlink from "./Hyperlink.vue";
import ValueFile from "./File.vue";
import ValueDate from "./Date.vue";
import ValueDateTime from "./DateTime.vue";
import RecordTableView from "../display/RecordTableView.vue";
import InlinePagination from "../display/InlinePagination.vue";
import ContentOntology from "../content/Ontology.vue";

const props = withDefaults(
  defineProps<{
    metadata: IColumn;
    data: any;
    hideListSeparator?: boolean;
  }>(),
  {
    hideListSeparator: false,
  }
);

const emit = defineEmits<{
  (e: "valueClick", payload: RefPayload): void;
}>();

// Single refs: REF, RADIO, SELECT
const isSingleRef = computed(() =>
  ["REF", "RADIO", "SELECT"].includes(props.metadata.columnType)
);

// Array refs: REF_ARRAY, REFBACK, MULTISELECT, CHECKBOX
const isArrayRef = computed(() =>
  ["REF_ARRAY", "REFBACK", "MULTISELECT", "CHECKBOX"].includes(
    props.metadata.columnType
  )
);

// Ontology types
const isOntology = computed(() =>
  ["ONTOLOGY", "ONTOLOGY_ARRAY"].includes(props.metadata.columnType)
);

// Display mode for array refs
const displayComponent = computed(() => {
  const comp = props.metadata.displayConfig?.component;
  return typeof comp === "string" ? comp : comp ? "custom" : "bullets";
});

const useTableMode = computed(() => {
  return (
    isArrayRef.value &&
    displayComponent.value === "table" &&
    visibleColumns.value.length > 0
  );
});

const visibleColumns = computed(() => {
  const config = props.metadata.displayConfig;
  if (!config?.visibleColumns) return [];
  const refMeta = props.metadata as IRefColumn;
  if (!refMeta.refTableMetadata?.columns) return [];
  return refMeta.refTableMetadata.columns.filter((c: IColumn) =>
    config.visibleColumns!.includes(c.id)
  );
});

const allRows = computed(() => (Array.isArray(props.data) ? props.data : []));

const ontologyTree = computed(() =>
  Array.isArray(props.data) ? props.data : props.data ? [props.data] : []
);

const effectivePageSize = computed(() => {
  return props.metadata.displayConfig?.pageSize || 5;
});

// Pagination state for inline list views
const listPage = ref(1);
const listTotalPages = computed(() =>
  Math.ceil(allRows.value.length / effectivePageSize.value)
);
const showListPagination = computed(
  () => allRows.value.length > effectivePageSize.value
);
const paginatedRows = computed(() =>
  allRows.value.slice(
    (listPage.value - 1) * effectivePageSize.value,
    listPage.value * effectivePageSize.value
  )
);

// Get ref label for array items
const refLabelTemplate = computed(
  () => props.metadata.refLabel || props.metadata.refLabelDefault || ""
);

// Handle click on array item
function handleArrayItemClick(row: IRow) {
  const clickAction = props.metadata.displayConfig?.clickAction;
  if (clickAction) {
    clickAction(props.metadata, row);
  }
}

// Click handler for single refs
function handleRefClick() {
  const clickAction = props.metadata.displayConfig?.clickAction;
  if (clickAction) {
    clickAction(props.metadata, props.data as IRow);
  } else {
    emit("valueClick", {
      metadata: props.metadata as IRefColumn,
      data: props.data,
    });
  }
}
</script>

<template>
  <template v-if="data == null || data === undefined"></template>

  <!-- Single refs: REF, RADIO, SELECT -->
  <template v-else-if="isSingleRef">
    <NuxtLink
      v-if="metadata.displayConfig?.getHref"
      :to="metadata.displayConfig.getHref(metadata, data as IRow)"
      class="underline text-link"
    >
      {{
        rowToString(data, metadata.refLabel || metadata.refLabelDefault || "")
      }}
    </NuxtLink>
    <span
      v-else-if="metadata.displayConfig?.clickAction"
      class="underline hover:cursor-pointer text-link"
      @click="handleRefClick"
    >
      {{
        rowToString(data, metadata.refLabel || metadata.refLabelDefault || "")
      }}
    </span>
    <span v-else>
      {{
        rowToString(data, metadata.refLabel || metadata.refLabelDefault || "")
      }}
    </span>
  </template>

  <div v-else-if="useTableMode">
    <RecordTableView :rows="paginatedRows" :columns="visibleColumns" />
    <InlinePagination
      v-if="showListPagination"
      :current-page="listPage"
      :total-pages="listTotalPages"
      @update:page="listPage = $event"
    />
  </div>

  <!-- Array refs: REF_ARRAY, REFBACK, MULTISELECT, CHECKBOX (bullets/list mode) -->
  <div v-else-if="isArrayRef" class="record-list-view">
    <ul
      v-if="paginatedRows.length"
      class="grid gap-1 pl-4 list-disc list-outside"
    >
      <li v-for="(row, index) in paginatedRows" :key="index">
        <NuxtLink
          v-if="metadata.displayConfig?.getHref"
          :to="metadata.displayConfig.getHref(metadata, row)"
          class="underline text-link"
        >
          {{ rowToString(row, refLabelTemplate) }}
        </NuxtLink>
        <span
          v-else-if="metadata.displayConfig?.clickAction"
          class="underline hover:cursor-pointer text-link"
          @click="handleArrayItemClick(row)"
        >
          {{ rowToString(row, refLabelTemplate) }}
        </span>
        <span v-else>{{ rowToString(row, refLabelTemplate) }}</span>
      </li>
    </ul>
    <p v-else class="text-gray-400 dark:text-gray-500 italic">No items</p>
    <InlinePagination
      v-if="showListPagination"
      :current-page="listPage"
      :total-pages="listTotalPages"
      @update:page="listPage = $event"
    />
  </div>

  <!-- Ontology types -->
  <ContentOntology v-else-if="isOntology" :tree="ontologyTree" />

  <!-- Array primitives -->
  <ValueList
    v-else-if="metadata.columnType.endsWith('ARRAY')"
    :metadata="metadata"
    :data="data"
    :hideListSeparator="hideListSeparator"
  />

  <!-- Primitives -->
  <ValueString
    v-else-if="metadata.columnType === 'STRING'"
    :metadata="metadata"
    :data="data"
  />

  <ValueText
    v-else-if="metadata.columnType === 'TEXT'"
    :metadata="metadata"
    :data="data"
  />

  <ValueDecimal
    v-else-if="metadata.columnType === 'DECIMAL'"
    :metadata="metadata"
    :data="data"
  />

  <ValueLong
    v-else-if="metadata.columnType === 'LONG'"
    :metadata="metadata"
    :data="typeof data === 'number' ? data : Number(data)"
  />

  <ValueInt
    v-else-if="
      metadata.columnType === 'INT' ||
      metadata.columnType === 'NON_NEGATIVE_INT'
    "
    :metadata="metadata"
    :data="typeof data === 'number' ? data : Number(data)"
  />

  <ValueBool
    v-else-if="metadata.columnType === 'BOOL'"
    :metadata="metadata"
    :data="data"
  />

  <ValueEmail
    v-else-if="metadata.columnType === 'EMAIL'"
    :metadata="metadata"
    :data="data"
  />

  <ValueHyperlink
    v-else-if="metadata.columnType === 'HYPERLINK'"
    :metadata="metadata"
    :data="data"
  />

  <ValueFile
    v-else-if="metadata.columnType === 'FILE'"
    :metadata="metadata"
    :data="data"
  />

  <ValueString
    v-else-if="metadata.columnType === 'AUTO_ID'"
    :metadata="metadata"
    :data="data"
  />

  <ValueString
    v-else-if="metadata.columnType === 'PERIOD'"
    :metadata="metadata"
    :data="data"
  />

  <ValueString
    v-else-if="metadata.columnType === 'UUID'"
    :metadata="metadata"
    :data="data"
  />

  <ValueDate
    v-else-if="metadata.columnType === 'DATE'"
    :metadata="metadata"
    :data="data"
  />

  <ValueDateTime
    v-else-if="metadata.columnType === 'DATETIME'"
    :metadata="metadata"
    :data="data"
  />

  <template v-else> Unknown type: {{ metadata.columnType }} </template>
</template>
