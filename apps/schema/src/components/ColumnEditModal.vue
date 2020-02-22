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
          :readonly="defaultValue != undefined"
        />
        <InputSelect
          v-model="column.columnType"
          label="Column type"
          :default-value="defaultValue ? defaultValue.columnType : undefined"
          :items="columnTypes"
        />
        <InputSelect
          v-if="column.columnType == 'REF'"
          v-model="column.refTable"
          label="Referenced table"
          :default-value="defaultValue ? defaultValue.refTable : undefined"
          :items="tables"
        />
        <!--InputSelect
          v-if="column.columnType == 'REF'"
          v-model="column.refColumn"
          label="Referenced column"
          :defaultValue="defaultValue ? defaultValue.refColumn : undefined"
          :items="columns"
        /-->
        <InputBoolean
          v-model="column.nullable"
          label="Nullable"
          :default-value="defaultValue && defaultValue.nullable ? true : false"
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
      <ButtonAction v-if="defaultValue" @click="executeCommand">{{
        action
      }}</ButtonAction>
      <ButtonAction v-else @click="executeCommand">{{ action }}</ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import { request } from 'graphql-request'

import {
  MessageSuccess,
  MessageError,
  ButtonAction,
  ButtonAlt,
  LayoutModal,
  InputBoolean,
  InputString,
  InputText,
  SigninForm,
  InputSelect,
  LayoutForm,
  Spinner
} from '@mswertz/emx2-styleguide'

const columnTypes = [
  'STRING',
  'INT',
  'BOOL',
  'DECIMAL',
  'DATE',
  'DATETIME',
  'REF',
  'REF_ARRAY',
  'UUID',
  'TEXT',
  'STRING_ARRAY',
  'INT_ARRAY',
  'BOOL_ARRAY',
  'DECIMAL_ARRAY',
  'DATE_ARRAY',
  'DATETIME_ARRAY',
  'UUID_ARRAY',
  'TEXT_ARRAY'
]

export default {
  components: {
    MessageSuccess,
    MessageError,
    ButtonAction,
    ButtonAlt,
    LayoutModal,
    InputBoolean,
    InputString,
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
    }
  },
  computed: {
    title() {
      if (this.defaultValue) {
        return `Alter column(${this.defaultValue.name}) in table '${this.table}'`
      } else return `Add column to table '${this.table}'`
    },
    action() {
      if (this.defaultValue) return `Alter column`
      else return `Add column`
    },
    endpoint() {
      return '/api/graphql/' + this.schema
    },
    tables() {
      return this.metadata.map(table => table.name)
    },
    columns() {
      if (this.column.refTable) {
        let columnList
        this.metadata.forEach(table => {
          if (table.name === this.column.refTable) columnList = table.columns
        })
        if (columnList) {
          return columnList.map(column => column.name)
        }
      }
      return []
    },
    tableMetadata() {
      return null
    }
  },
  watch: {
    column() {
      this.$emit('input', this.column)
    }
  },
  methods: {
    executeCommand() {
      this.loading = true
      this.error = null
      this.success = null
      let command = this.defaultValue ? 'alter' : 'add'
      request(
        this.endpoint,
        `mutation ${command}Column($table:String,$column:MolgenisColumnInput){${command}Column(table:$table,column:$column){message}}`,
        {
          table: this.table,
          column: this.column
        }
      )
        .then(data => {
          if (this.defaultValue) {
            this.tables = data.alterColumn.message
            this.success = `Column ${this.column.name} altered`
          } else {
            this.tables = data.addColumn.message
            this.success = `Column ${this.column.name} created`
          }
          this.$emit('close')
        })
        .catch(error => {
          if (error.response.status === 403) {
            this.error = 'Forbidden. Do you need to login?'
            this.showLogin = true
          } else this.error = error
        })
      this.loading = false
    }
  }
}
</script>

<docs>
Example:
```
<template>
  <div>
    <ButtonAction v-if="!show" @click="show = true">Show</ButtonAction>
    <ColumnEditModal v-else v-model="column" @close="close" />
    <br />
    Value: {{column}}
  </div>
</template>

<script>
export default {
  data: function() {
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
