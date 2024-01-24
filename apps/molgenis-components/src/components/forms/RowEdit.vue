<template>
  <div>
    <FormInput
      v-for="column in shownColumnsWithoutMeta"
      :key="JSON.stringify(column)"
      :id="`${id}-${column.id}`"
      :modelValue="internalValues[column.id]"
      :columnType="column.columnType"
      :description="column.description"
      :errorMessage="errorPerColumn[column.id]"
      :label="column.label"
      :schemaId="column.refSchemaId || schemaMetaData.id"
      :pkey="pkey"
      :readonly="
        column.readonly ||
        (pkey && column.key === 1 && !clone) ||
        (column.computed !== undefined && column.computed.trim() !== '')
      "
      :refBackId="column.refBackId"
      :refLabel="column.refLabel || column.refLabelDefault"
      :required="column.required"
      :tableId="column.refTableId"
      :canEdit="canEdit"
      :filter="refLinkFilter(column)"
      @update:modelValue="handleModelValueUpdate($event, column.id)"
    />
  </div>
</template>

<script setup lang="ts">
import type { IColumn, ISchemaMetaData, ITableMetaData } from "meta-data-utils";
import type { IRow } from "../../Interfaces/IRow";
import constants from "../constants.js";
import { deepClone } from "../utils";
import FormInput from "./FormInput.vue";
import { executeExpression, isColumnVisible } from "./formUtils/formUtils";
import { computed, ref, watch, withDefaults, defineProps } from "vue";

const { AUTO_ID } = constants;
const {
  modelValue,
  id,
  tableMetaData,
  schemaMetaData,
  applyDefaultValues,
  canEdit,
  clone,
  defaultValue,
  errorPerColumn,
  pkey,
  visibleColumns,
} = withDefaults(
  defineProps<{
    id: string;
    modelValue: Record<string, any>;
    pkey?: Record<string, any>;
    tableMetaData: ITableMetaData;
    schemaMetaData: ISchemaMetaData;
    applyDefaultValues?: boolean;
    canEdit?: boolean;
    clone?: Boolean;
    defaultValue?: Record<string, any>;
    errorPerColumn?: Record<string, any>;
    visibleColumns?: string[];
  }>(),
  {
    errorPerColumn: () => ({}),
    applyDefaultValues: false,
    canEdit: false,
  }
);
let internalValues = ref(deepClone(defaultValue ? defaultValue : modelValue));
const emit = defineEmits(["update:modelValue"]);

let shownColumnsWithoutMeta = computed(() => {
  const columnsWithoutMeta = tableMetaData?.columns
    ? tableMetaData.columns.filter(
        (column: IColumn) => !column.id?.startsWith("mg_")
      )
    : [];
  return columnsWithoutMeta.filter(showColumn);
});

tableMetaData.columns.forEach((column: IColumn) => {
  if (column.defaultValue && !internalValues[column.id]) {
    if (applyDefaultValues) {
      if (column.defaultValue.startsWith("=")) {
        try {
          internalValues[column.id] = executeExpression(
            "(" + column.defaultValue.substr(1) + ")",
            internalValues,
            tableMetaData as ITableMetaData
          );
        } catch (error) {
          errorPerColumn[column.id] =
            "Default value expression failed: " + error;
        }
      }
    } else {
      internalValues[column.id] = column.defaultValue;
    }
  }
});
onValuesUpdate();

function showColumn(column: IColumn) {
  if (column.columnType === AUTO_ID) {
    return pkey?.value;
  } else if (column.refLinkId) {
    return internalValues.value[column.refLinkId];
  } else {
    const isColumnVisible = visibleColumns
      ? visibleColumns.includes(column.id)
      : true;
    return (
      isColumnVisible && isVisible(column) && column.id !== "mg_tableclass"
    );
  }
}

function isVisible(column: IColumn) {
  try {
    return isColumnVisible(
      column,
      internalValues.value,
      tableMetaData as ITableMetaData
    );
  } catch (error: any) {
    errorPerColumn[column.id] = error;
    return true;
  }
}

function applyComputed() {
  tableMetaData.columns.forEach((column: IColumn) => {
    if (column.computed && column.columnType !== AUTO_ID) {
      try {
        internalValues.value[column.id] = executeExpression(
          column.computed,
          internalValues.value,
          tableMetaData as ITableMetaData
        );
      } catch (error) {
        errorPerColumn[column.id] = "Computation failed: " + error;
      }
    }
  });
}

//create a filter in case inputs are linked by overlapping refs
function refLinkFilter(column: IColumn) {
  //need to figure out what refs overlap
  if (
    column.refLinkId &&
    showColumn(column) &&
    internalValues[column.refLinkId]
  ) {
    let filter: Record<string, any> = {};
    tableMetaData.columns.forEach((column2: IColumn) => {
      if (column2.id === column.refLinkId) {
        schemaMetaData.tables.forEach((table: ITableMetaData) => {
          //check how the refTableId overlaps with columns in our column
          if (table.id === column.refTableId) {
            table.columns.forEach((column3) => {
              if (
                column3.key === 1 &&
                column3.refTableId === column2.refTableId
              ) {
                filter[column3.id] = {
                  //@ts-ignore
                  equals: internalValues[column.refLinkId],
                };
              }
            });
          }
        });
      }
    });
    return filter;
  }
}

function handleModelValueUpdate(event: any, columnId: string) {
  internalValues[columnId] = event;
  onValuesUpdate();
}

function onValuesUpdate() {
  applyComputed();
  emit("update:modelValue", internalValues);
}

watch(tableMetaData, onValuesUpdate, { deep: true });
</script>

<docs>
<template>
  <DemoItem>
    <div class="row">
      <div class="col-6">
        <label class="border-bottom">In create mode</label>
        <RowEdit
            v-if="showRowEdit"
            id="row-edit"
            v-model="rowData"
            :tableMetaData="tableMetadata"
            :schemaMetaData="schemaMetadata"
        />
      </div>
      <div class="col-6 border-left">
        <label for="create-mode-config" class="border-bottom">Meta data</label>
        <dl id="create-mode-config">
          <dt>Table name</dt>
          <dd>
            <select v-model="tableId">
              <option>Pet</option>
              <option>Order</option>
              <option>Category</option>
              <option>User</option>
            </select>
          </dd>
          <dt>Row data</dt>
          <dd>{{ rowData }}</dd>

          <dt>Metadata</dt>
          <dd>{{ tableMetadata }}</dd>
        </dl>
      </div>
    </div>
  </DemoItem>
</template>
<script>
  export default {
    data: function () {
      return {
        showRowEdit: true,
        tableId: 'Pet',
        tableMetadata: {
          columns: [],
        },
        schemaMetadata: {},
        rowData: {},
        schemaId: 'pet store',
      };
    },
    watch: {
      async tableId(newValue, oldValue) {
        if (newValue !== oldValue) {
          this.rowData = {};
          await this.reload();
        }
      },
    },
    methods: {
      async reload() {
        // force complete component reload to have a clean demo component and hit all lifecycle events
        this.showRowEdit = false;
        const client = this.$Client.newClient(this.schemaId);
        this.schemaMetadata = await client.fetchSchemaMetaData();
        this.tableMetadata = await client.fetchTableMetaData(this.tableId);
        //this.rowData = (await client.fetchTableData(this.tableId))[this.tableId];
        this.showRowEdit = true;
      },
    },
    async mounted() {
      this.reload();
    },
  };
</script>
</docs>
