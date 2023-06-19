<template>
  <div>
    <FormInput
      v-for="column in columnsWithoutMeta.filter(showColumn)"
      :key="JSON.stringify(column)"
      :id="`${id}-${column.name}`"
      :modelValue="internalValues[column.id]"
      :columnType="column.columnType"
      :description="getColumnDescription(column)"
      :errorMessage="errorPerColumn[column.id]"
      :label="getColumnLabel(column)"
      :schemaName="column.refSchema ? column.refSchema : schemaMetaData.name"
      :pkey="getPrimaryKey(internalValues, tableMetaData)"
      :readonly="
        column.readonly ||
        (pkey && column.key === 1 && !clone) ||
        (column.computed !== undefined && column.computed.trim() != '')
      "
      :refBack="column.refBack"
      :refTablePrimaryKeyObject="getPrimaryKey(internalValues, tableMetaData)"
      :refLabel="column.refLabel ? column.refLabel : column.refLabelDefault"
      :required="column.required"
      :tableName="column.refTable"
      :canEdit="canEdit"
      :filter="refLinkFilter(column)"
      @update:modelValue="handleModelValueUpdate($event, column.id)"
    />
  </div>
</template>

<script>
import FormInput from "./FormInput.vue";
import constants from "../constants.js";
import {
  getPrimaryKey,
  deepClone,
  convertToCamelCase,
  getLocalizedLabel,
  getLocalizedDescription,
} from "../utils";

const { AUTO_ID } = constants;

export default {
  name: "RowEdit",
  data: function () {
    return {
      internalValues: deepClone(this.modelValue),
      errorPerColumn: {},
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
      default: () => {},
    },
  },
  emits: ["update:modelValue", "errorsInForm"],
  components: {
    FormInput,
  },
  computed: {
    columnsWithoutMeta() {
      return this?.tableMetaData?.columns
        ? this.tableMetaData.columns.filter(
            (column) => !column.id?.startsWith("mg_")
          )
        : [];
    },
    graphqlFilter() {
      if (this.tableMetaData && this.pkey) {
        return this.tableMetaData.columns
          .filter((column) => column.key == 1)
          .reduce((accum, column) => {
            accum[column.id] = { equals: this.pkey[column.id] };
            return accum;
          }, {});
      } else {
        return {};
      }
    },
  },
  methods: {
    getPrimaryKey,
    getColumnLabel(column) {
      return getLocalizedLabel(column, this.locale);
    },
    getColumnDescription(column) {
      return getLocalizedDescription(column, this.locale);
    },
    showColumn(column) {
      if (column.columnType === AUTO_ID) {
        return this.pkey;
      } else if (column.reflink) {
        return this.internalValues[convertToCamelCase(column.refLink)];
      } else {
        const isColumnVisible = this.visibleColumns
          ? this.visibleColumns.includes(column.id)
          : true;
        return (
          isColumnVisible &&
          this.visible(column.visible, column.id) &&
          column.id !== "mg_tableclass"
        );
      }
    },
    visible(expression, columnId) {
      if (expression) {
        try {
          return executeExpression(
            expression,
            this.internalValues,
            this.tableMetaData
          );
        } catch (error) {
          this.errorPerColumn[
            columnId
          ] = `Invalid visibility expression, reason: ${error}`;
          return true;
        }
      } else {
        return true;
      }
    },
    applyComputed() {
      //apply computed
      this.tableMetaData.columns.forEach((c) => {
        if (c.computed && c.columnType !== AUTO_ID) {
          try {
            this.internalValues[c.id] = executeExpression(
              c.computed,
              this.internalValues,
              this.tableMetaData
            );
            this.onValuesUpdate();
          } catch (error) {
            this.errorPerColumn[c.id] = "Computation failed: " + error;
          }
        }
      });
    },
    //create a filter in case inputs are linked by overlapping refs
    refLinkFilter(c) {
      //need to figure out what refs overlap
      if (
        c.refLink &&
        this.showColumn(c) &&
        this.internalValues[convertToCamelCase(c.refLink)]
      ) {
        let filter = {};
        this.tableMetaData.columns.forEach((c2) => {
          if (c2.name === c.refLink) {
            this.schemaMetaData.tables.forEach((t) => {
              //check how the reftable overlaps with columns in our column
              if (t.name === c.refTable) {
                t.columns.forEach((c3) => {
                  if (c3.key === 1 && c3.refTable === c2.refTable) {
                    filter[c3.name] = {
                      equals:
                        this.internalValues[convertToCamelCase(c.refLink)],
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
    handleModelValueUpdate(event, columnId) {
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
            :tableMetaData="tableMetaData"
            :locale="locale"
            :schemaMetaData="schemaMetaData"
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

          <dt>MetaData</dt>
          <dd>{{ tableMetaData }}</dd>
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
        tableMetaData: {
          columns: [],
        },
        schemaMetaData: {},
        rowData: {},
        schemaName: 'pet store',
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
        this.schemaMetaData = await client.fetchSchemaMetaData();
        this.tableMetaData = await client.fetchTableMetaData(this.tableName);
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
