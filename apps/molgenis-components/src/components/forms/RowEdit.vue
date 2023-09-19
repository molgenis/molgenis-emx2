<template>
  <div>
    <FormInput
      v-for="column in columnsWithoutMeta.filter(showColumn)"
      :key="JSON.stringify(column)"
      :id="`${id}-${column.name}`"
      :modelValue="internalValues[column.name]"
      :columnType="column.columnType"
      :description="getColumnDescription(column)"
      :errorMessage="errorPerColumn[column.name]"
      :label="getColumnLabel(column)"
      :schemaName="column.refSchema ? column.refSchema : schemaMetaData.name"
      :pkey="primaryKey"
      :readonly="
        column.readonly ||
        (pkey && column.key === 1 && !clone) ||
        (column.computed !== undefined && column.computed.trim() != '')
      "
      :refBack="column.refBack"
      :refTablePrimaryKeyObject="primaryKey"
      :refLabel="column.refLabel ? column.refLabel : column.refLabelDefault"
      :required="column.required"
      :tableName="column.refTable"
      :canEdit="canEdit"
      :filter="refLinkFilter(column)"
      @update:modelValue="handleModelValueUpdate($event, column.name)"
    />
  </div>
</template>

<script lang="ts">
import FormInput from "./FormInput.vue";
import constants from "../constants.js";
import Client from "../../client/client";
import {
  deepClone,
  getLocalizedLabel,
  getLocalizedDescription,
} from "../utils";
import { IColumn } from "../../Interfaces/IColumn";
import { IRow } from "../../Interfaces/IRow";
import { executeExpression, isColumnVisible } from "./formUtils/formUtils";
import { ITableMetaData } from "../../Interfaces/ITableMetaData";

const { AUTO_ID } = constants;

export default {
  name: "RowEdit",
  data: function () {
    return {
      client: Client.newClient(this.schemaMetaData.name),
      internalValues: deepClone(this.modelValue),
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
    // tableName: name of the molgenis table loaded
    tableName: {
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
    locale: {
      type: String,
      default: () => "en",
    },
    errorPerColumn: {
      type: Object,
      default: () => ({}),
    },
  },
  emits: ["update:modelValue", "errorsInForm"],
  components: {
    FormInput,
  },
  computed: {
    async primaryKey() {
      return this.client.convertRowToPrimaryKey(
        this.internalValues,
        this.tableName
      );
    },
    columnsWithoutMeta() {
      return this?.tableMetaData?.columns
        ? this.tableMetaData.columns.filter(
            (column: IColumn) => !column.name?.startsWith("mg_")
          )
        : [];
    },
    graphqlFilter() {
      if (this.tableMetaData && this.pkey) {
        return this.tableMetaData.columns
          .filter((column: IColumn) => column.key === 1)
          .reduce(
            (accum: Record<string, { equals: IRow }>, column: IColumn) => {
              accum[column.name] = {
                equals: this.pkey ? this.pkey[column.name] : undefined,
              };
              return accum;
            },
            {}
          );
      } else {
        return {};
      }
    },
  },
  methods: {
    getColumnLabel(column: IColumn) {
      return getLocalizedLabel(column, this.locale);
    },
    getColumnDescription(column: IColumn) {
      return getLocalizedDescription(column, this.locale);
    },
    showColumn(column: IColumn) {
      if (column.columnType === AUTO_ID) {
        return this.pkey;
      } else if (column.refLink) {
        return this.internalValues[column.refLink];
      } else {
        const isColumnVisible = this.visibleColumns
          ? this.visibleColumns.includes(column.name)
          : true;

        return (
          isColumnVisible &&
          this.isVisible(column) &&
          column.name !== "mg_tableclass"
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
        this.errorPerColumn[column.name] = error;
        return true;
      }
    },
    applyComputed() {
      this.tableMetaData.columns.forEach((column: IColumn) => {
        if (column.computed && column.columnType !== AUTO_ID) {
          try {
            this.internalValues[column.name] = executeExpression(
              column.computed,
              this.internalValues,
              this.tableMetaData as ITableMetaData
            );
          } catch (error) {
            this.errorPerColumn[column.name] = "Computation failed: " + error;
          }
        }
      });
    },
    //create a filter in case inputs are linked by overlapping refs
    refLinkFilter(column: IColumn) {
      //need to figure out what refs overlap
      if (
        column.refLink &&
        this.showColumn(column) &&
        this.internalValues[column.refLink]
      ) {
        let filter: Record<string, any> = {};
        this.tableMetaData.columns.forEach((column2: IColumn) => {
          if (column2.name === column.refLink) {
            this.schemaMetaData.tables.forEach((table: ITableMetaData) => {
              //check how the reftable overlaps with columns in our column
              if (table.name === column.refTable) {
                table.columns.forEach((column3) => {
                  if (
                    column3.key === 1 &&
                    column3.refTable === column2.refTable
                  ) {
                    filter[column3.name] = {
                      equals: this.internalValues[column.refLink],
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
    handleModelValueUpdate(event: any, columnName: string) {
      this.internalValues[columnName] = event;
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
    if (this.defaultValue) {
      this.internalValues = deepClone(this.defaultValue);
    }
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
            :tableName="tableName"
            :tableMetaData="tableMetadata"
            :locale="locale"
            :schemaMetaData="schemaMetadata"
        />
      </div>
      <div class="col-6 border-left">
        <label for="create-mode-config" class="border-bottom">Meta data</label>
        <dl id="create-mode-config">
          <dt>Table name</dt>
          <dd>
            <select v-model="tableName">
              <option>Pet</option>
              <option>Order</option>
              <option>Category</option>
              <option>User</option>
            </select>
          </dd>
          <InputString v-model="locale" label="locale" id="locale"/>
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
        locale: 'en',
        tableName: 'Pet',
        tableMetadata: {
          columns: [],
        },
        schemaMetadata: {},
        rowData: {},
        schemaName: 'petStore',
      };
    },
    watch: {
      async tableName(newValue, oldValue) {
        if (newValue !== oldValue) {
          this.rowData = {};
          await this.reload();
        }
      },
      async locale(newValue, oldValue) {
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
        const client = this.$Client.newClient(this.schemaName);
        this.schemaMetadata = await client.fetchSchemaMetaData();
        this.tableMetadata = await client.fetchTableMetaData(this.tableName);
        // this.rowData = (await client.fetchTableData(this.tableName))[this.tableName];
        this.showRowEdit = true;
      },
    },
    async mounted() {
      this.reload();
    },
  };
</script>
</docs>
