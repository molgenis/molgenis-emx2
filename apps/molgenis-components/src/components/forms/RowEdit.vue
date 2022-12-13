<template>
  <div>
    <FormInput
      v-for="column in columnsWithoutMeta.filter(showColumn)"
      :key="column.name"
      :id="`${id}-${column.name}`"
      v-model="internalValues[column.id]"
      :columnType="column.columnType"
      :description="column.description"
      :errorMessage="errorPerColumn[column.id]"
      :graphqlURL="graphqlURL"
      :label="column.name"
      :pkey="getPrimaryKey(internalValues, tableMetaData)"
      :readonly="column.readonly || (pkey && column.key == 1 && !clone)"
      :refBack="column.refBack"
      :refTablePrimaryKeyObject="getPrimaryKey(internalValues, tableMetaData)"
      :refLabel="column.refLabel"
      :required="column.required"
      :tableName="column.refTable"
      :canEdit="canEdit"
    />
  </div>
</template>

<script>
import FormInput from "./FormInput.vue";
import constants from "../constants";
import {getPrimaryKey, deepClone, convertToCamelCase} from "../utils";
import Expressions from "@molgenis/expressions";

const { EMAIL_REGEX, HYPERLINK_REGEX } = constants;

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
    visibleColumns: {
      type: Array,
      required: false,
    },
    // defaultValue: when creating new record, this is initialization value
    defaultValue: {
      type: Object,
      required: false,
    },
    // graphqlURL: url to graphql endpoint
    graphqlURL: {
      default: "graphql",
      type: String,
    },
    canEdit: {
      type: Boolean,
      required: false,
      default: () => true,
    },
  },
  emits: ["update:modelValue"],
  components: {
    FormInput,
  },
  computed: {
    columnsWithoutMeta() {
      return this?.tableMetaData?.columns
        ? this.tableMetaData.columns.filter(
            (column) => !column.name.startsWith("mg_")
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
    showColumn(column) {
      const isColumnVisible = Array.isArray(this.visibleColumns)
        ? this.visibleColumns.map((column) => column.name).includes(column.name)
        : true;

      console.log(column.name + " "+column.computed)
      return (
        (isColumnVisible &&
          this.visible(column.visible, column.id) &&
          column.name !== "mg_tableclass" &&
          !column.refLink && !column.computed) ||
        this.internalValues[column.refLink]
      );
    },
    visible(expression, columnId) {
      if (expression) {
        try {
          return Expressions.evaluate(expression, this.internalValues);
        } catch (error) {
          this.errorPerColumn[columnId] = `Invalid visibility expression`;
        }
      } else {
        return true;
      }
    },
    validateTable() {
      this.tableMetaData?.columns
        ?.filter((column) => {
          if (this.visibleColumns) {
            return this.visibleColumns.find(
              (visibleColumn) => column.name === visibleColumn.name
            );
          } else {
            return true;
          }
        })
        .forEach((column) => {
          this.errorPerColumn[column.id] = getColumnError(
            column,
            this.internalValues,
            this.tableMetaData
          );
        });
    },
  },
  watch: {
    internalValues: {
      handler(newValue) {
        this.validateTable();
        this.$emit("update:modelValue", newValue);
      },
      deep: true,
    },
    tableMetaData: {
      handler() {
        this.validateTable();
      },
      deep: true,
    },
  },
  created() {
    if (this.defaultValue) {
      this.internalValues = deepClone(this.defaultValue);
    }
    this.validateTable();
  },
};

function getColumnError(column, values, tableMetaData) {
  const value = values[column.id];
  const isInvalidNumber = typeof value === "number" && isNaN(value);
  const missesValue = value === undefined || value === null || value === "";
  const type = column.columnType;

  if (column.required && (missesValue || isInvalidNumber)) {
    return column.name + " is required ";
  }
  if (missesValue) {
    return undefined;
  }
  if (type === "EMAIL" && !isValidEmail(value)) {
    return "Invalid email address";
  }
  if (type === "EMAIL_ARRAY" && containsInvalidEmail(value)) {
    return "Invalid email address";
  }
  if (type === "HYPERLINK" && !isValidHyperlink(value)) {
    return "Invalid hyperlink";
  }
  if (type === "HYPERLINK_ARRAY" && containsInvalidHyperlink(value)) {
    return "Invalid hyperlink";
  }
  if (column.validation) {
    return evaluateValidationExpression(column, values);
  }
  if (isRefLinkWithoutOverlap(column, tableMetaData, values)) {
    return `value should match your selection in column '${column.refLink}' `;
  }

  return undefined;
}

function evaluateValidationExpression(column, values) {
  try {
    if (!Expressions.evaluate(column.validation, values)) {
      return `Applying validation rule returned error: ${column.validation}`;
    } else {
      return undefined;
    }
  } catch (error) {
    return "Invalid validation expression, reason: " + error.ha;
  }
}

function isRefLinkWithoutOverlap(column, tableMetaData, values) {
  if (!column.refLink) {
    return false;
  }
  const columnRefLink = column.refLink;
  const refLinkId = convertToCamelCase(columnRefLink);

  const value = values[column.id];
  const refValue = values[refLinkId];

  if (typeof value === "string" && typeof refValue === "string") {
    return value && refValue && value !== refValue;
  } else {
    return (
      value &&
      refValue &&
      !JSON.stringify(value).includes(JSON.stringify(refValue))
    );
  }
}

function isValidHyperlink(value) {
  return HYPERLINK_REGEX.test(String(value).toLowerCase());
}

function containsInvalidHyperlink(hyperlinks) {
  return hyperlinks.find((hyperlink) => !this.isValidHyperlink(hyperlink));
}

function isValidEmail(value) {
  return EMAIL_REGEX.test(String(value).toLowerCase());
}

function containsInvalidEmail(emails) {
  return emails.find((email) => !this.isValidEmail(email));
}
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
            :graphqlURL="graphqlURL"
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
    data: function() {
      return {
        showRowEdit: true,
        tableName: 'Pet',
        tableMetaData: {
          columns: [],
        },
        rowData: {},
        graphqlURL: '/pet store/graphql',
      };
    },
    watch: {
      async tableName(newValue, oldValue) {
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
        const client = this.$Client.newClient(this.graphqlURL);
        this.tableMetaData = (await client.fetchMetaData()).tables.find(
            (table) => table.id === this.tableName,
        );
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
