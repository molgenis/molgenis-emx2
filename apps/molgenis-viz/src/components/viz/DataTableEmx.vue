<template>
  <LoadingScreen v-if="tableLoading" message="" />
  <MessageBox v-if="tableError" type="error">
    <output>{{ tableError }}</output>
  </MessageBox>
  <DataTable
    v-if="tableSuccess"
    :tableId="tableId"
    :data="tableData"
    :columnOrder="columnOrder"
    :enable-row-highlighting="enableRowHighlighting"
    :enable-row-clicks="enableRowClicks"
    :render-html="renderHtml"
    @row-clicked="onClick"
  />
</template>

<script setup lang="ts">
import { ref, onBeforeMount, watch, computed } from "vue";
import { gql } from "graphql-tag";
import { request } from "graphql-request";

import { setGraphQlEndpoint } from "../../utils";
import type {
  DataTableParams,
  gqlVariableSubSelectionIF,
} from "../../interfaces/viz";
import {
  buildQuery,
  gqlExtractSelectionName,
  gqlExtractSubSelectionNames,
  prepareChartData,
} from "../../utils/emxViz";

import DataTable from "./DataTable.vue";
import LoadingScreen from "../display/LoadingScreen.vue";
import MessageBox from "../display/MessageBox.vue";

const props = withDefaults(defineProps<DataTableParams>(), {
  renderHtml: true,
  enableRowHighlighting: true,
});

const emit = defineEmits<{
  (e: "viz-data-clicked", row: object): void;
}>();

const graphqlEndpoint = ref<string | null>(null);

const tableLoading = ref<boolean>(true);
const tableSuccess = ref<boolean>(false);
const tableError = ref<Error | null>(null);
const tableData = ref<object[]>([]);
const tableDataQuery = ref<string | null>(null);

const tableColumnMappings = ref<gqlVariableSubSelectionIF[] | null>(null);
const columnOrder = ref<string[] | null>(null);

function setChartVariables() {
  graphqlEndpoint.value = setGraphQlEndpoint(props.schema);

  tableDataQuery.value = buildQuery({
    table: props.table,
    selections: [props.columns],
  });

  const selections =
    tableDataQuery.value.definitions[0].selectionSet?.selections[0].selectionSet
      ?.selections;
  tableColumnMappings.value = selections.map((selection: object) => {
    return {
      key: selection.name?.value,
      nestedKey: selection.selectionSet?.selections[0].name?.value,
    };
  });

  columnOrder.value = tableColumnMappings.value.map((row: Object) => row.key);
}

async function fetchChartData() {
  tableLoading.value = true;
  tableSuccess.value = false;
  tableError.value = null;

  try {
    const response = await request(graphqlEndpoint.value, tableDataQuery.value);
    const data = await response[props.table as string];
    tableData.value = await prepareChartData({
      data: data,
      chartVariables: [...tableColumnMappings.value],
    });
    tableSuccess.value = true;
  } catch (error) {
    tableError.value = error;
  } finally {
    tableLoading.value = false;
  }
}

function onClick(data: object): object | null {
  if (props.enableRowClicks) {
    emit("viz-data-clicked", data);
  }
}

onBeforeMount(() => setChartVariables());
watch(props, () => setChartVariables());
watch(tableDataQuery, async () => await fetchChartData());
</script>
