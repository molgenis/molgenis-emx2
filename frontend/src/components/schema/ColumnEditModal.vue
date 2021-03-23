<template>
  <!-- when authorisation error show login-->
  <Spinner v-if="loading" />
  <div v-else-if="showLogin">
    <MessageError v-if="graphqlError">
      {{ graphqlError }}
    </MessageError>
    <SigninForm @cancel="cancel" @login="loginSuccess" />
  </div>
  <!-- when update succesfull show result before close -->
  <LayoutModal
    v-else-if="success"
    :show="true"
    :title="title"
    @close="$emit('close')"
  >
    <template #body>
      <MessageSuccess>{{ success }}</MessageSuccess>
    </template>
    <template #footer>
      <ButtonAction @click="$emit('close')">
        Close
      </ButtonAction>
    </template>
  </LayoutModal>
  <!-- alter or add a column -->
  <LayoutModal
    v-else :show="true"
    :title="title"
    @close="$emit('close')"
  >
    <template #body>
      <ColumnEdit v-model="column" :table="tableMetadata" :tables="tables" />
    </template>
    <template #footer>
      <MessageSuccess v-if="success">
        {{ success }}
      </MessageSuccess>
      <MessageError v-if="graphqlError">
        {{ graphqlError }}
      </MessageError>
      <ButtonAlt @click="$emit('close')">
        Close
      </ButtonAlt>
      <ButtonAction
        v-if="defaultValue"
        :disabled="column.name == undefined || column.name == ''"
        @click="change"
      >
        {{ action }}
      </ButtonAction>
      <ButtonAction v-else @click="change">
        {{ action }}
      </ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import ColumnEdit from './ColumnEdit'
import {request} from 'graphql-request'
import {
  ButtonAction,  ButtonAlt, LayoutModal, MessageError,
  MessageSuccess, SigninForm, Spinner,
} from '@/components/ui/index.js'

const columnTypes = [
  'STRING',
  'INT',
  'BOOL',
  'DECIMAL',
  'DATE',
  'DATETIME',
  'REF',
  'REF_ARRAY',
  // depcrecated "MREF",
  'REFBACK',
  'UUID',
  'TEXT',
  'STRING_ARRAY',
  'INT_ARRAY',
  'BOOL_ARRAY',
  'DECIMAL_ARRAY',
  'DATE_ARRAY',
  'DATETIME_ARRAY',
  'UUID_ARRAY',
  'TEXT_ARRAY',
]

export default {
  components: {
    ButtonAction,
    ButtonAlt,
    ColumnEdit,
    LayoutModal,
    MessageError,
    MessageSuccess,
    SigninForm,
    Spinner,
  },
  props: {
    defaultValue: Object,
    metadata: Array,
    schema: String,
    table: String,
  },
  emits: ['close', 'input'],
  data: function() {
    return {
      column: {},
      columnTypes,
      graphqlError: null,
      key: 0,
      loading: false,
      showLogin: false,
      success: null,
    }
  },
  computed: {
    action() {
      if (this.defaultValue) return 'Alter column'
      else return 'Add column'
    },
    columns() {
      if (this.column.refTable) {
        let columnList
        this.metadata.forEach((table) => {
          if (table.name === this.column.refTable) columnList = table.columns
        })
        if (columnList) {
          return columnList.map((column) => column.name)
        }
      }
      return []
    },
    tableMetadata() {
      return this.metadata.filter((table) => table.name == this.table)[0]
    },
    tables() {
      return this.metadata.map((table) => table.name)
    },
    title() {
      if (this.defaultValue) {
        return `Alter column(${this.defaultValue.name}) in table '${this.table}'`
      } else return `Add column to table '${this.table}'`
    },
  },

  watch: {
    column() {
      this.$emit('input', this.column)
    },
  },
  created() {
    if (this.defaultValue) {
      // todo refactor to follow v-model rules
      this.column = this.defaultValue
      this.column.oldName = this.column.name
    }
  },
  methods: {
    change() {
      this.loading = true
      this.graphqlError = null
      this.success = null
      this.column.table = this.table
      request(
        'graphql',
        'mutation change($column:MolgenisColumnInput){change(columns:[$column]){message}}',
        {
          column: this.column,
        },
      )
        .then((data) => {
          this.tables = data.change.message
          this.success = `Column ${this.column.name} created/altered`
          this.$emit('close')
        })
        .catch((graphqlError) => {
          if (graphqlError.response.status === 403) {
            this.graphqlError = 'Forbidden. Do you need to login?'
            this.showLogin = true
          } else {
            this.graphqlError = graphqlError.response.errors[0].message
          }
        })
        .finally((this.loading = false))
    },
  },
}
</script>
