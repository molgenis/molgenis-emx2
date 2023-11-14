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

<script lang="ts">
import { IColumn } from "../../Interfaces/IColumn";
import { IRow } from "../../Interfaces/IRow";
import { ITableMetaData } from "../../Interfaces/ITableMetaData";
import constants from "../constants.js";
import { deepClone } from "../utils";
import FormInput from "./FormInput.vue";
import { executeExpression, isColumnVisible } from "./formUtils/formUtils";

const { AUTO_ID } = constants;

export default {
  name: "RowEdit",
  data() {
    return {
      internalValues: deepClone(
        this.defaultValue ? this.defaultValue : this.modelValue
      ),
    };
  },
  props: {
    modelValue: {
      type: Object,
      required: true,
    },
    // id: used as html id for components
    id: {
      type: String,
      required: true,
    },
    // tableId: name of the molgenis table loaded
    tableId: {
      type: String,
      required: true,
    },
    // defines the form structure
    tableMetaData: {
      type: Object,
      required: true,
    },
    // pkey:  when updating existing record, this is the primary key value
    pkey: { type: Object },
    // clone: when you want to clone instead of update
    clone: {
      type: Boolean,
      required: false,
    },
    // visibleColumns:  visible columns, useful if you only want to allow partial edit (column of object)
    // examples ['name','description']
    visibleColumns: {
      type: Array,
      required: false,
    },
    // defaultValue: when creating new record, this is initialization value
    defaultValue: {
      type: Object,
      required: false,
    },
    // object with the whole schema, needed to create refLink filter
    schemaMetaData: {
      type: Object,
      required: true,
    },
    canEdit: {
      type: Boolean,
      required: false,
      default: () => true,
    },
    errorPerColumn: {
      type: Object,
      default: () => ({}),
    },
    applyDefaultValues: {
      type: Boolean,
      default: () => false,
    },
  },
  emits: ["update:modelValue"],
  components: {
    FormInput,
  },
  computed: {
    shownColumnsWithoutMeta() {
      const columnsWithoutMeta = this?.tableMetaData?.columns
        ? this.tableMetaData.columns.filter(
            (column: IColumn) => !column.id?.startsWith("mg_")
          )
        : [];
      return columnsWithoutMeta.filter(this.showColumn);
    },
    graphqlFilter() {
      if (this.tableMetaData && this.pkey) {
        return this.tableMetaData.columns
          .filter((column: IColumn) => column.key === 1)
          .reduce(
            (accum: Record<string, { equals: IRow }>, column: IColumn) => {
              accum[column.id] = {
                equals: this.pkey ? this.pkey[column.id] : undefined,
              };
              return accum;
            }
          );
      } else {
        return {};
      }
    },
  },
  methods: {
    showColumn(column: IColumn) {
      if (column.columnType === AUTO_ID) {
        return this.pkey;
      } else if (column.refLinkId) {
        return this.internalValues[column.refLinkId];
      } else {
        const isColumnVisible = this.visibleColumns
          ? this.visibleColumns.includes(column.id)
          : true;

        return (
          isColumnVisible &&
          this.isVisible(column) &&
          column.id !== "mg_tableclass"
        );
      }
    },
    isVisible(column: IColumn) {
      try {
        return isColumnVisible(
          column,
          this.internalValues,
          this.tableMetaData as ITableMetaData
        );
      } catch (error: any) {
        this.errorPerColumn[column.id] = error;
        return true;
      }
    },
    applyComputed() {
      this.tableMetaData.columns.forEach((column: IColumn) => {
        if (column.computed && column.columnType !== AUTO_ID) {
          try {
            this.internalValues[column.id] = executeExpression(
              column.computed,
              this.internalValues,
              this.tableMetaData as ITableMetaData
            );
          } catch (error) {
            this.errorPerColumn[column.id] = "Computation failed: " + error;
          }
        } else if (this.applyDefaultValues && column.defaultValue) {
          if (column.defaultValue.startsWith("=")) {
            this.internalValues[column.id] = executeExpression(
              "(" + column.defaultValue.substr(1) + ")",
              this.internalValues,
              this.tableMetaData as ITableMetaData
            );
          }
        }
      });
    },
    //create a filter in case inputs are linked by overlapping refs
    refLinkFilter(column: IColumn) {
      //need to figure out what refs overlap
      if (
        column.refLinkId &&
        this.showColumn(column) &&
        this.internalValues[column.refLinkId]
      ) {
        let filter: Record<string, any> = {};
        this.tableMetaData.columns.forEach((column2: IColumn) => {
          if (column2.id === column.refLinkId) {
            this.schemaMetaData.tables.forEach((table: ITableMetaData) => {
              //check how the refTableId overlaps with columns in our column
              if (table.id === column.refTableId) {
                table.columns.forEach((column3) => {
                  if (
                    column3.key === 1 &&
                    column3.refTableId === column2.refTableId
                  ) {
                    filter[column3.id] = {
                      //@ts-ignore
                      equals: this.internalValues[column.refLinkId],
                    };
                  }
                });
              }
            });
          }
        });
        return filter;
      }
    },
    handleModelValueUpdate(event: any, columnId: string) {
      this.internalValues[columnId] = event;
      this.onValuesUpdate();
    },
    onValuesUpdate() {
      this.applyComputed();
      this.$emit("update:modelValue", this.internalValues);
    },
  },
  watch: {
    tableMetaData: {
      handler() {
        this.onValuesUpdate();
      },
      deep: true,
    },
  },
  created() {
    this.tableMetaData.columns.forEach((column: IColumn) => {
      if (column.defaultValue && !this.internalValues[column.id]) {
        this.internalValues[column.id] = column.defaultValue;
      }
    });
    this.onValuesUpdate();
  },
};
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
            :tableId="tableId"
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
