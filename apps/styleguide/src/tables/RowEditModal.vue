<template>
  <div v-if="showLogin">
    <MessageError v-if="error">{{ error }}</MessageError>
    <SigninForm @login="loginSuccess" @cancel="cancel" />
  </div>
  <LayoutModal v-else :title="title" :show="true" @close="$emit('close')">
    <template v-slot:body>
      <LayoutForm v-if="tableMetadata && (pkey == null || value)" :key="value">
        <span v-for="column in tableMetadata.columns" :key="column.name">
          <RowFormInput
            v-if="
              visible(column.visibleExpression) &&
              column.name != 'mg_tableclass'
            "
            v-model="value[column.name]"
            :label="column.name"
            :help="column.description"
            :columnType="column.columnType"
            :refTable="column.refTable"
            :required="column.required"
            :error="errorPerColumn[column.name]"
            :readonly="column.readonly || (pkey && column.key == 1)"
            :graphqlURL="graphqlURL"
          />
        </span>
      </LayoutForm>
      <ShowMore title="debug">
        <pre>

value={{ JSON.stringify(value) }}

data={{ JSON.stringify(data) }}

graphql = {{ JSON.stringify(graphql) }}

filter = {{ JSON.stringify(filter) }}

errorPerColumn = {{ JSON.stringify(errorPerColumn) }}

schema = {{ JSON.stringify(schema) }}


        </pre>
      </ShowMore>
    </template>
    <template v-slot:footer>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-if="error">{{ error }}</MessageError>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonAction @click="executeCommand">{{ title }}</ButtonAction>
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
import SigninForm from "../layout/MolgenisSignin";
import TableMixin from "../mixins/TableMixin";
import GraphqlRequestMixin from "../mixins/GraphqlRequestMixin";
import RowFormInput from "./RowFormInput.vue";
import ShowMore from "../layout/ShowMore";

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
    ShowMore,
  },
  methods: {
    reload() {
      //override superclass
      if (this.pkey) {
        TableMixin.methods.reload.call(this);
      }
    },
    loginSuccess() {
      this.error = null;
      this.success = null;
      this.showLogin = false;
    },
    executeCommand() {
      this.error = null;
      this.success = null;
      // todo spinner
      let name = this.table;
      let variables = { value: [this.value] };
      let query = `mutation insert($value:[${name}Input]){insert(${name}:$value){message}}`;
      if (this.pkey) {
        query = `mutation update($value:[${name}Input]){update(${name}:$value){message}}`;
      }
      this.requestMultipart(this.graphqlURL, query, variables)
        .then((data) => {
          if (data.insert) {
            this.success = data.insert.message;
          }
          if (data.update) {
            this.success = data.update.message;
          }
          this.defaultValue = this.value;
          this.$emit("close");
        })
        .catch((error) => {
          if (error.status === 403) {
            this.error =
              "Schema doesn't exist or permission denied. Do you need to Sign In?";
            this.showLogin = true;
          } else {
            this.error = error.errors;
          }
        });
    },
    eval(expression) {
      try {
        return eval("(function (row) { " + expression + "})")(this.value); // eslint-disable-line
      } catch (e) {
        return "Script error contact admin: " + e.message;
      }
    },
    visible(expression) {
      if (expression) {
        return this.eval(expression);
      } else {
        return true;
      }
    },
    validate() {
      if (this.selectedTable) {
        this.selectedTable.columns.forEach((column) => {
          // make really empty if empty
          if (/^\s*$/.test(this.value[column.name])) {
            //this.value[column.name] = null;
          }
          delete this.errorPerColumn[column.name];
          // when empty
          if (
            this.value[column.name] == null ||
            (typeof this.value[column.name] === "number" &&
              isNaN(this.value[column.name]))
          ) {
            // when required
            if (column.required) {
              this.errorPerColumn[column.name] = column.name + " is required ";
            }
          } else {
            // when not empty
            // when validation
            if (
              typeof this.value[column.name] !== "undefined" &&
              typeof column.validationExpression !== "undefined"
            ) {
              let value = this.value[column.name]; //used for eval, two lines below
              this.errorPerColumn[column.name] = value; //dummy assign
              this.errorPerColumn[column.name] = this.eval(
                column.validationExpression
              );
            }
          }
        });
      }
    },
  },
  computed: {
    // override from tableMixin
    filter() {
      let result = {};
      if (this.tableMetadata && this.pkey) {
        this.tableMetadata.columns
          .filter((c) => c.key == 1)
          .map((c) => (result[c.name] = { equals: this.pkey[c.name] }));
      }
      return result;
    },
    title() {
      if (this.pkey) {
        return `update ${this.table}`;
      } else {
        return `insert ${this.table}`;
      }
    },
  },
  watch: {
    data(val) {
      //TODO prevent loading of parent class if no pkey
      if (this.pkey && val && val.length > 0) {
        let data = val[0];
        let defaultValue = {};
        this.tableMetadata.columns.forEach((column) => {
          if (data[column.name]) {
            defaultValue[column.name] = data[column.name];
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
  },
  created() {
    this.validate();
  },
};
</script>
