<template>
  <LayoutModal :title="title" :show="true" @close="$emit('close')">
    <template v-slot:body>
      <LayoutForm v-if="tableMetaData && (pkey == null || value)">
        <span v-for="column in columnsWithoutMeta" :key="column.name">
          <FormInput
            v-if="showColumn(column)"
            v-model="value[column.id]"
            :columnType="column.columnType"
            :description="column.description"
            :errorMessage="errorPerColumn[column.id]"
            :graphqlURL="graphqlURL"
            :label="column.name"
            :pkey="getPrimaryKey(value)"
            :readonly="column.readonly || (pkey && column.key == 1 && !clone)"
            :refBack="column.refBack"
            :refLabel="column.refLabel"
            :required="column.required"
            :tableName="column.refTable"
          />
        </span>
      </LayoutForm>
    </template>
    <template v-slot:footer>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonOutline @click="saveDraft">Save draft</ButtonOutline>
      <ButtonAction @click="save">Save {{ tableName }}</ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import Client from "../../client/client.js";
import LayoutForm from "../layout/LayoutForm.vue";
import LayoutModal from "../layout/LayoutModal.vue";
import MessageError from "./MessageError.vue";
import MessageSuccess from "./MessageSuccess.vue";
import ButtonAction from "./ButtonAction.vue";
import ButtonAlt from "./ButtonAlt.vue";
import ButtonOutline from "./ButtonOutline.vue";
import FormInput from "./FormInput.vue";
import Expressions from "@molgenis/expressions";
import constants from "../constants";
import { getPrimaryKey } from "../utils";

const { EMAIL_REGEX, HYPERLINK_REGEX } = constants;

/**
 * Properties:
 * pkey:  when updating existing record, this is the primary key value
 * clone: when you want to clone instead of update
 * visibleColumns:  visible columns, useful if you only want to allow partial edit (array of strings)
 * defaultValue: when creating new record, this is initialization value
 * **/

export default {
  name: "EditModal",
  data: function () {
    return {
      showLogin: false,
      value: {},
      errorPerColumn: {},
      success: null,
      tableMetaData: null,
      data: null,
      graphqlError: null,
      client: null,
    };
  },
  props: {
    pkey: Object,
    clone: Boolean,
    visibleColumns: Array,
    defaultValue: Object,
    graphqlURL: {
      default: "graphql",
      type: String,
    },
    tableName: {
      type: String,
      required: true,
    },
  },
  components: {
    LayoutForm,
    FormInput,
    ButtonAction,
    ButtonAlt,
    LayoutModal,
    MessageError,
    MessageSuccess,
    ButtonOutline,
  },
  methods: {
    saveDraft() {
      this.save(true);
    },
    save(isDraft) {
      let promise;
      if (this.pkey && !this.clone) {
        promise = this.client.updateVariables(
          this.graphqlURL,
          this.tableName,
          isDraft,
          this.value
        );
      } else {
        promise = this.client.insertVariables(
          this.graphqlURL,
          this.tableName,
          isDraft,
          this.value
        );
      }
      promise
        .then((data) => {
          if (data.insert) {
            this.success = data.insert.message;
          }
          if (data.update) {
            this.success = data.update.message;
          }
          this.$emit("close");
        })
        .catch((error) => {
          if (error.status === 403) {
            this.graphqlError =
              "Schema doesn't exist or permission denied. Do you need to Sign In?";
          } else {
            this.graphqlError = error.errors[0].message;
          }
        });
    },
    showColumn(column) {
      const hasRefValue = !column.refLink || this.value[column.refLink];
      const isColumnVisible =
        !this.visibleColumns || this.visibleColumns.includes(column.name);
      return (
        isColumnVisible &&
        this.visible(column.visible, column.id) &&
        column.name != "mg_tableclass" &&
        hasRefValue
      );
    },
    visible(expression, columnId) {
      if (expression) {
        try {
          return Expressions.evaluate(expression, this.value);
        } catch (error) {
          this.errorPerColumn[columnId] = `Invalid visibility expression`;
        }
      } else {
        return true;
      }
    },
    validate() {
      if (this.tableMetaData) {
        this.tableMetaData.columns.forEach((column) =>
          this.validateColumn(column)
        );
      }
    },
    validateColumn(column) {
      const value = this.value[column.id];
      const isInvalidNumber = typeof value === "number" && isNaN(value);
      const missesValue = value === undefined || value === null || value === "";
      if (column.required && (missesValue || isInvalidNumber)) {
        this.errorPerColumn[column.id] = column.name + " is required ";
      } else if (missesValue) {
        this.errorPerColumn[column.id] = undefined;
      } else {
        this.errorPerColumn[column.id] = this.getColumnError(
          column,
          this.values
        );
      }
    },
    getColumnError,
    isRefLinkWithoutOverlap(column) {
      if (!column.refLink) {
        return false;
      } else {
        const refLinkId = getRefLinkColumnByName(
          this.tableMetaData,
          column.refLink
        ).id;
        const value = this.value[column.id];
        const refValue = this.value[refLinkId];

        if (typeof value === "string" && typeof refValue === "string") {
          return value && refValue && value !== refValue;
        } else {
          return (
            value &&
            refValue &&
            JSON.stringify(value) !== JSON.stringify(refValue)
          );
        }
      }
    },
    getPrimaryKey,
  },
  computed: {
    columnsWithoutMeta() {
      return this.tableMetaData.columns.filter(
        (column) => !column.name.startsWith("mg_")
      );
    },
    //@overide
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
    // override from tableMixin
    title() {
      if (this.pkey && this.clone) {
        return `copy ${this.tableName}`;
      } else if (this.pkey) {
        return `update ${this.tableName}`;
      } else {
        return `insert ${this.tableName}`;
      }
    },
  },
  watch: {
    data(val) {
      // TODO: prevent loading of parent class if no pkey
      if (this.pkey && val && val.length > 0) {
        let data = val[0];
        let defaultValue = {};
        this.tableMetaData.columns.forEach((column) => {
          // primary skip (key=1) key in case of clone
          if (data[column.id] && (!this.clone || column.key != 1)) {
            defaultValue[column.id] = data[column.id];
          }
        });
        this.value = defaultValue;
      }
    },
    // validation happens here
    value: {
      handler() {
        this.validate();
      },
      deep: true,
    },
    tableMetaData: {
      handler() {
        this.validate();
      },
      deep: true,
    },
  },
  async mounted() {
    this.client = Client.newClient(this.graphqlURL);
    this.tableMetaData = (await this.client.fetchMetaData()).tables.find(
      (table) => table.id === this.tableName
    );
    const response = await this.client.fetchTableData(this.tableName);
    this.data = response[this.tableName];
  },
  created() {
    //pass by value
    if (this.defaultValue) {
      this.value = JSON.parse(JSON.stringify(this.defaultValue));
    }
    this.validate();
  },
};

function getColumnError(column, values) {
  const value = values[column.id];
  const type = column.columnType;

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
  if (this.isRefLinkWithoutOverlap(column)) {
    return `value should match your selection in column '${column.refLink}' `;
  }

  return undefined;
}

function isValidHyperlink(value) {
  return HYPERLINK_REGEX.test(String(value).toLowerCase());
}

function containsInvalidHyperlink(hyperlinks) {
  return hyperlinks.find((hyperlink) => !isValidHyperlink(hyperlink));
}

function isValidEmail(value) {
  return EMAIL_REGEX.test(String(value).toLowerCase());
}

function containsInvalidEmail(emails) {
  return emails.find((email) => !isValidEmail(email));
}

function evaluateValidationExpression(column, values) {
  try {
    if (!Expressions.evaluate(column.validation, values)) {
      return `Applying validation rule returned error: ${column.validation}`;
    }
  } catch (error) {
    return "Invalid validation expression";
  }
}

function getRefLinkColumnByName(tableMetaData, refLink) {
  return tableMetaData.columns.find((column) => column.name === refLink);
}
</script>


<docs>
  <template>
    <DemoItem>
      <EditModal
          :pkey="{name:'Pet'}"
          graphqlURL="/pet store/graphql"
          tableName="Pet"
      />
      You typed: {{ JSON.stringify(value) }}
    </DemoItem>
  </template>
  <script>
  export default {
    data: function () {
      return {
        value: null,
      };
    },
  };
  </script>
</docs>
