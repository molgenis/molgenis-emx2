<template>
  <!-- when authorisation error show login-->
  <Spinner v-if="loading" />
  <div v-else-if="showLogin">
    <MessageError v-if="error">{{ error }}</MessageError>
    <SigninForm @cancel="cancel" @login="loginSuccess" />
  </div>
  <!-- when update succesfull show result before close -->
  <LayoutModal
    v-else-if="success"
    :show="true"
    :title="title"
    @close="$emit('close')"
  >
    <template v-slot:body>
      <MessageSuccess>{{ success }}</MessageSuccess>
    </template>
    <template v-slot:footer>
      <ButtonAction @click="$emit('close')">Close</ButtonAction>
    </template>
  </LayoutModal>
  <!-- alter or add a column -->
  <LayoutModal v-else :show="true" :title="title" @close="$emit('close')">
    <template v-slot:body>
      <ColumnEdit v-model="column" :table="tableMetadata" :tables="tables" />
    </template>
    <template v-slot:footer>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-if="error">{{ error }}</MessageError>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonAction
        v-if="defaultValue"
        :disabled="column.name == undefined || column.name == ''"
        @click="createOrAlter"
        >{{ action }}
      </ButtonAction>
      <ButtonAction v-else @click="createOrAlter">{{ action }}</ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import { request } from "graphql-request";

import {
  ButtonAction,
  ButtonAlt,
  InputBoolean,
  InputInt,
  InputSelect,
  InputString,
  InputText,
  LayoutForm,
  LayoutModal,
  MessageError,
  MessageSuccess,
  SigninForm,
  Spinner,
} from "@mswertz/emx2-styleguide";
import ColumnEdit from "./ColumnEdit";

const columnTypes = [
  "STRING",
  "INT",
  "BOOL",
  "DECIMAL",
  "DATE",
  "DATETIME",
  "REF",
  "REF_ARRAY",
  //depcrecated "MREF",
  "REFBACK",
  "UUID",
  "TEXT",
  "STRING_ARRAY",
  "INT_ARRAY",
  "BOOL_ARRAY",
  "DECIMAL_ARRAY",
  "DATE_ARRAY",
  "DATETIME_ARRAY",
  "UUID_ARRAY",
  "TEXT_ARRAY",
];

export default {
  components: {
    ColumnEdit,
    MessageSuccess,
    MessageError,
    ButtonAction,
    ButtonAlt,
    LayoutModal,
    InputBoolean,
    InputString,
    InputInt,
    InputText,
    InputSelect,
    LayoutForm,
    Spinner,
    SigninForm,
  },
  props: {
    schema: String,
    table: String,
    metadata: Array,
    defaultValue: Object,
  },
  data: function () {
    return {
      key: 0,
      column: {},
      loading: false,
      columnTypes,
      error: null,
      success: null,
      showLogin: false,
    };
  },
  computed: {
    title() {
      if (this.defaultValue) {
        return `Alter column(${this.defaultValue.name}) in table '${this.table}'`;
      } else return `Add column to table '${this.table}'`;
    },
    action() {
      if (this.defaultValue) return `Alter column`;
      else return `Add column`;
    },
    tables() {
      return this.metadata.map((table) => table.name);
    },
    columns() {
      if (this.column.refTable) {
        let columnList;
        this.metadata.forEach((table) => {
          if (table.name === this.column.refTable) columnList = table.columns;
        });
        if (columnList) {
          return columnList.map((column) => column.name);
        }
      }
      return [];
    },
    tableMetadata() {
      return this.metadata.filter((table) => table.name == this.table)[0];
    },
  },
  created() {
    if (this.defaultValue) {
      //todo refactor to follow v-model rules
      this.column = this.defaultValue;
      this.column.oldName = this.column.name;
      this.column.command = "ALTER";
    } else {
      this.column.command = "CREATE";
    }
  },

  watch: {
    column() {
      this.$emit("input", this.column);
    },
  },
  methods: {
    createOrAlter() {
      this.loading = true;
      this.error = null;
      this.success = null;
      this.column.table = this.table;
      request(
        "graphql",
        `mutation createOrAlter($column:MolgenisColumnInput){createOrAlter(columns:[$column]){message}}`,
        {
          column: this.column,
        }
      )
        .then((data) => {
          this.tables = data.createOrAlter.message;
          this.success = `Column ${this.column.name} created/altered`;
          this.$emit("close");
        })
        .catch((error) => {
          if (error.response.status === 403) {
            this.error = "Forbidden. Do you need to login?";
            this.showLogin = true;
          } else {
            this.error = error.response.errors[0].message;
            console.err(JSON.stringify(error));
          }
        })
        .finally((this.loading = false));
    },
  },
};
</script>

<docs>
Example:
```
<template>
  <div>
    <ButtonAction v-if="!show" @click="show = true">Show</ButtonAction>
    <ColumnEditModal v-else v-model="column" @close="close"/>
    <br/>
    Value: {{ column }}
  </div>
</template>

<script>
  export default {
    data: function () {
      return {
        show: false,
        column: {}
      };
    },
    methods: {
      close() {
        this.show = false;
      }
    }
  };
</script>
```
</docs>
