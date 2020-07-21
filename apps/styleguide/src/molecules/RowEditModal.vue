<template>
  <div v-if="showLogin">
    <MessageError v-if="error">{{ error }}</MessageError>
    <SigninForm @login="loginSuccess" @cancel="cancel" />
  </div>
  <LayoutModal v-else :title="title" :show="true" @close="$emit('close')">
    <template v-slot:body>
      <LayoutForm v-if="metadata && (pkey == null || defaultValue)">
        <span v-for="column in metadata.columns" :key="column.name">
          <RowFormInput
            v-model="value[column.name]"
            :label="column.name"
            :columnType="column.columnType"
            :refTable="column.refTable"
            :nullable="column.nullable"
            :defaultValue="defaultValue ? defaultValue[column.name] : undefined"
            :error="errorPerColumn[column.name]"
            :readonly="column.readonly || (pkey && column.key == 1)"
          />
        </span>
      </LayoutForm>
      defaultValue={{ JSON.stringify(defaultValue) }}
      <br />
      value={{ JSON.stringify(value) }}
      <br />
      data={{ JSON.stringify(data) }}
      <br />
      graphql = {{ JSON.stringify(graphql) }}
      <br />
      filter = {{ JSON.stringify(filter) }}
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
import LayoutForm from "../components/LayoutForm.vue";
import LayoutModal from "../components/LayoutModal.vue";
import MessageError from "../components/MessageError";
import MessageSuccess from "../components/MessageSuccess";
import ButtonAction from "../components/ButtonAction.vue";
import ButtonAlt from "../components/ButtonAlt.vue";
import SigninForm from "./SigninForm";
import TableMixin from "../mixins/TableMixin";
import RowFormInput from "./RowFormInput.vue";
import { request } from "graphql-request";

export default {
  mixins: [TableMixin],
  data: function() {
    return {
      showLogin: false,
      value: {},
      errorPerColumn: {},
      success: null,
      defaultValue: null
    };
  },
  props: {
    /** when updating existing record, this is the primary key value */
    pkey: Object
  },
  components: {
    LayoutForm,
    RowFormInput,
    ButtonAction,
    ButtonAlt,
    LayoutModal,
    MessageError,
    MessageSuccess,
    SigninForm
  },
  methods: {
    loginSuccess() {
      this.error = null;
      this.success = null;
      this.showLogin = false;
    },
    executeCommand() {
      this.error = null;
      this.success = null;
      // todo spinner
      let name = this.metadata.name;
      let variables = { value: [this.value] };
      let query = `mutation insert($value:[${name}Input]){insert(${name}:$value){message}}`;
      if (this.pkey) {
        query = `mutation update($value:[${name}Input]){update(${name}:$value){message}}`;
      }
      request("graphql", query, variables)
        .then(data => {
          if (data.insert) {
            this.success = data.insert.message;
          }
          if (data.update) {
            this.success = data.update.message;
          }
          this.defaultValue = this.value;
          this.$emit("close");
        })
        .catch(error => {
          if (error.response.status === 403) {
            this.error =
              "Schema doesn't exist or permission denied. Do you need to Sign In?";
            this.showLogin = true;
          } else {
            this.error = error;
          }
        });
    },
    validate() {
      if (this.metadata.columns) {
        this.metadata.columns.forEach(column => {
          // make really empty if empty
          if (/^\s*$/.test(this.value[column.name])) {
            //this.value[column.name] = undefined;
          }
          delete this.errorPerColumn[column.name];
          // when empty
          if (this.value[column.name] == null) {
            // when required
            if (column.nullable !== true) {
              this.errorPerColumn[column.name] = column.name + " is required ";
            }
          } else {
            // when not empty
            // when validation
            if (
              typeof this.value[column.name] !== "undefined" &&
              typeof column.validation !== "undefined"
            ) {
              let value = this.value[column.name]; //used for eval, two lines below
              this.errorPerColumn[column.name] = value; //dummy assign
              this.errorPerColumn[column.name] = eval(column.validation); // eslint-disable-line
            }
          }
        });
      }
    }
  },
  computed: {
    // override from tableMixin
    filter() {
      let result = {};
      if (this.metadata.columns && this.pkey) {
        this.metadata.columns
          .filter(c => c.key == 1)
          .map(c => (result[c.name] = { equals: this.pkey[c.name] }));
      }
      return result;
    },
    title() {
      if (this.pkey) {
        return `update ${this.metadata.name}`;
      } else {
        return `insert ${this.metadata.name}`;
      }
    }
  },
  watch: {
    data(val) {
      if (val && val.length > 0) {
        let data = val[0];
        let defaultValue = {};
        this.metadata.columns.forEach(column => {
          if (data[column.name]) {
            defaultValue[column.name] = data[column.name];
          }
        });
        this.defaultValue = defaultValue;
      }
    },
    // validation happens here
    value: {
      handler() {
        this.validate();
      },
      deep: true
    }
  },
  created() {
    this.validate();
  }
};
</script>
