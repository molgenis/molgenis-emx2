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
      <LayoutForm :key="key">
        {{ defaultValue }}
        <InputString
          v-model="column.name"
          :default-value="defaultValue ? defaultValue.name : undefined"
          :error="
            column.name == undefined || column.name == ''
              ? 'Name is required'
              : undefined
          "
          label="Name"
        />
        <InputText
          v-model="column.description"
          :default-value="defaultValue ? defaultValue.description : undefined"
          label="Description"
        />
        <h4>Constraints</h4>
        <InputSelect
          v-model="column.columnType"
          :default-value="defaultValue ? defaultValue.columnType : undefined"
          :options="columnTypes"
          label="Column type"
        />
        <InputSelect
          v-if="column.columnType == 'STRING'"
          v-model="column.columnFormat"
          :default-value="defaultValue ? defaultValue.columnFormat : undefined"
          :options="['', 'HYPERLINK']"
          label="Column format"
        />
        <InputSelect
          v-if="
            column.columnType == 'REF' ||
            column.columnType == 'REF_ARRAY' ||
            column.columnType == 'MREF' ||
            column.columnType == 'REFBACK'
          "
          v-model="column.refTable"
          :defaultValue="defaultValue ? defaultValue.refTable : undefined"
          :error="
            column.refTable == undefined || column.name == ''
              ? 'Referenced table is required'
              : undefined
          "
          :options="tables"
          label="Referenced table"
        />
        <InputSelect
          v-model="column.key"
          :options="[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"
          :default-value="
            defaultValue && defaultValue.key ? defaultValue.key : undefined
          "
          label="Key"
        />
        <InputBoolean
          v-model="column.nullable"
          :default-value="defaultValue && defaultValue.nullable ? true : false"
          label="Nullable"
        />
        <div
          v-if="
            column.columnType == 'REF' ||
            column.columnType == 'REF_ARRAY' ||
            column.columnType == 'MREF' ||
            column.columnType == 'REFBACK'
          "
        >
          <h4>Advanced relationship settings</h4>
          <InputString
            v-model="column.refSchema"
            label="refSchema (only needed if referencing outside schema)"
            :defaultValue="defaultValue ? defaultValue.refSchema : undefined"
          />
          <InputString
            v-if="column.columnType == 'REFBACK'"
            v-model="column.mappedBy"
            :default-value="defaultValue ? defaultValue.mappedBy : undefined"
            label="Mapped by"
          />
          <InputString
            v-if="
              column.columnType == 'REF' ||
              column.columnType == 'REF_ARRAY' ||
              column.columnType == 'MREF' ||
              column.columnType == 'REFBACK'
            "
            v-model="column.refFrom"
            :default-value="defaultValue ? defaultValue.refFrom : undefined"
            :list="true"
            label="refFrom"
          />
          <InputString
            v-if="
              column.columnType == 'REF' ||
              column.columnType == 'REF_ARRAY' ||
              column.columnType == 'MREF' ||
              column.columnType == 'REFBACK'
            "
            v-model="column.refTo"
            :default-value="defaultValue ? defaultValue.refTo : undefined"
            :list="true"
            label="refTo"
          />
        </div>
        <h4>Expressions</h4>
        <InputText
          v-model="column.validationExpression"
          :default-value="
            defaultValue ? defaultValue.validationExpression : undefined
          "
          label="Validation"
        />
        <InputText
          v-model="column.visibleExpression"
          :default-value="
            defaultValue ? defaultValue.visibleExpression : undefined
          "
          label="Visible"
        />
        <h4>Settings for semantic web</h4>
        <InputText
          v-model="column.jsonldType"
          :default-value="defaultValue ? defaultValue.jsonldType : undefined"
          label="jsonldType (should be valid json conform jsonld @type spec)"
        />
      </LayoutForm>
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
      return null;
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
