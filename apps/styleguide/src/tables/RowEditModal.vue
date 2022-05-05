<template>
  <div v-if="showLogin">
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <SigninForm @login="loginSuccess" @cancel="cancel" />
  </div>
  <LayoutModal v-else :title="title" :show="true" @close="$emit('close')">
    <template v-slot:body>
      <LayoutForm v-if="tableMetadata && (pkey == null || value)">
        <span v-for="column in columnsWithoutMeta" :key="column.name">
          <RowFormInput
            v-if="showColumn(column)"
            v-model="value[column.id]"
            :columnType="column.columnType"
            :description="column.description"
            :errorMessage="errorPerColumn[column.id]"
            :graphqlURL="graphqlURL"
            :label="column.name"
            :pkey="getPkey(value)"
            :readonly="column.readonly || (pkey && column.key == 1 && !clone)"
            :refBack="column.refBack"
            :refBackType="getRefBackType(column)"
            :refLabel="column.refLabel"
            :required="column.required"
            :table="column.refTable"
          />
        </span>
      </LayoutForm>
    </template>
    <template v-slot:footer>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonOutline @click="saveDraft">Save draft</ButtonOutline>
      <ButtonAction @click="save">Save {{ table }}</ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import LayoutForm from "../layout/LayoutForm.vue";
import LayoutModal from "../layout/LayoutModal.vue";
import MessageError from "../forms/MessageError";
import MessageSuccess from "../forms/MessageSuccess";
import ButtonAction from "../forms/ButtonAction.vue";
import ButtonAlt from "../forms/ButtonAlt.vue";
import ButtonOutline from "../forms/ButtonOutline";
import SigninForm from "../layout/MolgenisSignin";
import TableMixin from "../mixins/TableMixin";
import GraphqlRequestMixin from "../mixins/GraphqlRequestMixin";
import RowFormInput from "./RowFormInput.vue";
import Expressions from "@molgenis/expressions";
import { EMAIL_REGEX, HYPERLINK_REGEX } from "../constants";

export default {
  extends: TableMixin,
  mixins: [GraphqlRequestMixin],
  data: function () {
    return {
      showLogin: false,
      value: {},
      errorPerColumn: {},
      success: null,
    };
  },
  props: {
    /** when updating existing record, this is the primary key value */
    pkey: Object,
    /** when you want to clone instead of update */
    clone: Boolean,
    /** visible columns, useful if you only want to allow partial edit (array of strings) */
    visibleColumns: Array,
    /** when creating new record, this is initialization value */
    defaultValue: Object,
  },
  components: {
    LayoutForm,
    RowFormInput,
    ButtonAction,
    ButtonAlt,
    LayoutModal,
    MessageError,
    MessageSuccess,
    SigninForm,
    ButtonOutline,
  },
  methods: {
    getRefBackType(column) {
      if (column.columnType === "REFBACK") {
        const table = this.getTable(column.refTable);
        return table.columns.find(
          (otherColumn) => otherColumn.name === column.refBack
        ).columnType;
      }
    },
    reload() {
      //override superclass
      if (this.pkey) {
        TableMixin.methods.reload.call(this);
      }
    },
    loginSuccess() {
      this.graphqlError = null;
      this.success = null;
      this.showLogin = false;
    },
    saveDraft() {
      this.executeSaveCommand(true);
    },
    save() {
      this.executeSaveCommand(false);
    },
    executeSaveCommand(isDraft) {
      this.graphqlError = null;
      this.success = null;
      // TODO: add spinner

      this.setDraft(isDraft);

      const variables = { value: [this.value] };
      const query = this.getUpsertQuery();
      this.requestMultipart(this.graphqlURL, query, variables)
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
            this.showLogin = true;
          } else {
            this.graphqlError = error.errors[0].message;
          }
        });
    },
    setDraft(isDraft) {
      if (isDraft) {
        this.value["mg_draft"] = true;
      } else {
        this.value["mg_draft"] = false;
      }
    },
    getUpsertQuery() {
      const name = this.tableId;
      const action = this.pkey && !this.clone ? "update" : "insert";
      return `mutation ${action}($value:[${name}Input]){${action}(${name}:$value){message}}`;
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
      if (this.tableMetadata) {
        this.tableMetadata.columns.forEach((column) =>
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
        this.errorPerColumn[column.id] = this.getColumnError(column);
      }
    },
    getColumnError(column) {
      const value = this.value[column.id];
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
        return evaluateValidationExpression(column, this.value);
      }
      if (this.isRefLinkWithoutOverlap(column)) {
        return `value should match your selection in column '${column.refLink}' `;
      }

      return undefined;
    },
    isRefLinkWithoutOverlap(column) {
      if (!column.refLink) {
        return false;
      } else {
        const refLinkId = getRefLinkColumnByName(
          this.tableMetadata,
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
  },
  computed: {
    columnsWithoutMeta() {
      return this.tableMetadata.columns.filter(
        (c) => !c.name.startsWith("mg_")
      );
    },
    //@overide
    graphqlFilter() {
      if (this.tableMetadata && this.pkey) {
        return this.tableMetadata.columns
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
        return `copy ${this.table}`;
      } else if (this.pkey) {
        return `update ${this.table}`;
      } else {
        return `insert ${this.table}`;
      }
    },
  },
  watch: {
    data(val) {
      // TODO: prevent loading of parent class if no pkey
      if (this.pkey && val && val.length > 0) {
        let data = val[0];
        let defaultValue = {};
        this.tableMetadata.columns.forEach((column) => {
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
    tableMetadata: {
      handler() {
        this.validate();
      },
      deep: true,
    },
  },
  created() {
    //pass by value
    if (this.defaultValue) {
      this.value = JSON.parse(JSON.stringify(this.defaultValue));
    }
    this.validate();
  },
};

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

function getRefLinkColumnByName(tableMetadata, refLink) {
  return tableMetadata.columns.find((column) => column.name === refLink);
}
</script>
