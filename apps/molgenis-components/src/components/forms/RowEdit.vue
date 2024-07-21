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
      :filter="refFilter[column.id]"
      @update:modelValue="handleModelValueUpdate($event, column)"
    />
  </div>
</template>

<script lang="ts">
import { IColumn, ITableMetaData } from "meta-data-utils";
import constants from "../constants.js";
import { deepClone } from "../utils";
import FormInput from "./FormInput.vue";
import { executeExpression, isColumnVisible } from "./formUtils/formUtils";
import { convertRowToPrimaryKey } from "../../client/client";

const { AUTO_ID } = constants;

export default {
  name: "RowEdit",
  data() {
    return {
      internalValues: deepClone(
        this.defaultValue ? this.defaultValue : this.modelValue
      ),
      refFilter: {},
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
        console.log("isVisible expression error: ", error);
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
            console.log("Computed expression failed:", error);
            this.errorPerColumn[column.id] =
              "Computed expression failed: " + error;
          }
        }
      });
    },
    //update reflink filters and reset values if reflink changes
    async updateRefLinks(changedColumn: IColumn) {
      //need to figure out what refs overlap with the changed column
      for (const column1 of this.tableMetaData.columns) {
        if (
          this.internalValues[changedColumn.id] &&
          column1.refLinkId == changedColumn.id &&
          this.showColumn(column1)
        ) {
          console.log("check");
          //check how the refTableId overlaps with columns in our column
          for (const table of this.schemaMetaData.tables) {
            if (table.id === column1.refTableId) {
              console.log("check2");
              for (const column2 of table.columns) {
                if (
                  column2.key === 1 &&
                  column2.refTableId === changedColumn.refTableId
                ) {
                  //reset the value and the filter
                  this.internalValues[column1.id] = null;
                  //and then define new filter setting
                  this.refFilter[column1.id] = {};
                  this.refFilter[column1.id] = {
                    [column2.id]: {
                      //@ts-ignore
                      equals: await convertRowToPrimaryKey(
                        this.internalValues[changedColumn.id],
                        column2.refTableId,
                        column2.refSchemaId
                      ),
                    },
                  };
                }
              }
            }
          }
        }
      }
    },
    handleModelValueUpdate(newValue: any, column: IColumn) {
      this.internalValues[column.id] = newValue;
      this.updateRefLinks(column);
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
      if (
        this.applyDefaultValues &&
        column.defaultValue &&
        !this.internalValues[column.id]
      ) {
        if (column.defaultValue.startsWith("=")) {
          try {
            this.internalValues[column.id] = executeExpression(
              "(" + column.defaultValue.substr(1) + ")",
              this.internalValues,
              this.tableMetaData as ITableMetaData
            );
          } catch (error) {
            this.errorPerColumn[column.id] =
              "Default value expression failed: " + error;
          }
        } else {
          this.internalValues[column.id] = column.defaultValue;
        }
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
            :applyDefaultValues="true"
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
