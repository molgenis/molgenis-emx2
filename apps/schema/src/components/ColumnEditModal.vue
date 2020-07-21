<template>
  <!-- when authorisation error show login-->
  <Spinner v-if="loading" />
  <div v-else-if="showLogin">
    <MessageError v-if="error">{{ error }}</MessageError>
    <SigninForm @login="loginSuccess" @cancel="cancel" />
  </div>
  <!-- when update succesfull show result before close -->
  <LayoutModal
    v-else-if="success"
    :title="title"
    :show="true"
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
  <LayoutModal v-else :title="title" :show="true" @close="$emit('close')">
    <template v-slot:body>
      <LayoutForm :key="key">
        <InputString
          v-model="column.name"
          label="Column name"
          :default-value="defaultValue ? defaultValue.name : undefined"
          :error="
            column.name == undefined || column.name == ''
              ? 'Name is required'
              : undefined
          "
        />
        <InputSelect
          v-model="column.columnType"
          label="Column type"
          :default-value="defaultValue ? defaultValue.columnType : undefined"
          :options="columnTypes"
        />
        <InputSelect
          v-if="
            column.columnType == 'REF' ||
              column.columnType == 'REF_ARRAY' ||
              column.columnType == 'MREF' ||
              'REFBACK'
          "
          v-model="column.refTable"
          label="Referenced table"
          :defaultValue="defaultValue ? defaultValue.refTable : undefined"
          :options="tables"
          :error="
            column.refTable == undefined || column.name == ''
              ? 'Referenced table is required'
              : undefined
          "
        />
        <InputString
          v-if="column.columnType == 'REFBACK'"
          v-model="column.mappedBy"
          label="Mapped by"
          :default-value="defaultValue ? defaultValue.mappedBy : undefined"
        />
        <InputBoolean
          v-model="column.nullable"
          label="Nullable"
          :default-value="defaultValue && defaultValue.nullable ? true : false"
        />
        <InputInt
          v-model="column.key"
          label="Key"
          :default-value="
            defaultValue && defaultValue.key ? defaultValue.key : undefined
          "
        />
        <InputBoolean
          v-if="column.columnType == 'REF'"
          v-model="column.cascadeDelete"
          label="cascadeDelete"
          :default-value="
            defaultValue && defaultValue.cascadeDelete ? true : false
          "
        />
        <InputText
          v-model="column.description"
          label="Description"
          :default-value="defaultValue ? defaultValue.description : undefined"
        />
      </LayoutForm>
    </template>
    <template v-slot:footer>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-if="error">{{ error }}</MessageError>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonAction
        v-if="defaultValue"
        @click="executeCommand"
        :disabled="column.name == undefined || column.name == ''"
        >{{ action }}
      </ButtonAction>
      <ButtonAction v-else @click="executeCommand">{{ action }}</ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import { request } from "graphql-request";

import {
  ButtonAction,
  ButtonAlt,
  InputBoolean,
  InputSelect,
  InputString,
  InputInt,
  InputText,
  LayoutForm,
  LayoutModal,
  MessageError,
  MessageSuccess,
  SigninForm,
  Spinner
} from "@mswertz/emx2-styleguide";

const columnTypes = [
  "STRING",
  "INT",
  "BOOL",
  "DECIMAL",
  "DATE",
  "DATETIME",
  "REF",
  "REF_ARRAY",
  "MREF",
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
  "TEXT_ARRAY"
];

export default {
  components: {
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
    SigninForm
  },
  props: {
    schema: String,
    table: String,
    metadata: Array,
    defaultValue: Object
  },
  data: function() {
    return {
      key: 0,
      column: {},
      loading: false,
      columnTypes,
      error: null,
      success: null,
      showLogin: false
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
      return this.metadata.map(table => table.name);
    },
    columns() {
      if (this.column.refTable) {
        let columnList;
        this.metadata.forEach(table => {
          if (table.name === this.column.refTable) columnList = table.columns;
        });
        if (columnList) {
          return columnList.map(column => column.name);
        }
      }
      return [];
    },
    tableMetadata() {
      return null;
    }
  },
  watch: {
    column() {
      this.$emit("input", this.column);
    }
  },
  methods: {
    create() {
      this.loading = true;
      this.error = null;
      this.success = null;
      this.column.table = this.table;
      request(
        "graphql",
        `mutation create($column:MolgenisColumnInput){create(columns:[$column]){message}}`,
        {
          column: this.column
        }
      )
        .then(data => {
          this.tables = data.create.message;
          this.success = `Column ${this.column.name} created`;
          this.$emit("close");
        })
        .catch(error => {
          if (error.response.status === 403) {
            this.error = "Forbidden. Do you need to login?";
            this.showLogin = true;
          } else this.error = error;
        })
        .finally((this.loading = false));
    },
    alter() {
      this.loading = true;
      this.error = null;
      this.success = null;
      this.column.table = this.table;
      request(
        "graphql",
        `mutation alter($table:String,$name:String,$definition:MolgenisColumnInput){alter(columns:[{table:$table,name:$name,definition:$definition}]){message}}`,
        {
          table: this.table,
          name: this.defaultValue.name,
          definition: this.column
        }
      )
        .then(data => {
          this.tables = data.alter.message;
          this.success = `Column ${this.column.name} altered`;
          this.$emit("close");
        })
        .catch(error => {
          if (error.response.status === 403) {
            this.error = "Forbidden. Do you need to login?";
            this.showLogin = true;
          } else this.error = error.response.errors[0].message;
        })
        .finally((this.loading = false));
    },
    executeCommand() {
      this.defaultValue ? this.alter() : this.create();
    }
  }
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
            Value: {{column}}
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
