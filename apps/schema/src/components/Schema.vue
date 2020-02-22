<template>
  <Spinner v-if="loading" />
  <MessageError v-else-if="error">{{ error }}</MessageError>
  <LayoutCard v-else-if="tables" title="View and edit schema">
    <InputBoolean
      v-model="showAttributes"
      label="show attributes"
      :defaultValue="showAttributes"
    />
    <div style="overflow: auto; text-align:center" id="__top">
      <img :src="yuml" :key="showAttributes" />
      test
    </div>
    |
    <span v-for="table in tables" :key="table.name">
      <a :href="'#' + table.name">{{ table.name }}</a> |
    </span>
    <table class="table table-hover">
      <tbody v-for="table in tables" :key="table.name">
        <tr>
          <td colspan="3">
            <h1 :id="table.name">
              {{ table.name }}
              <IconBar class="hover">
                <IconAction icon="edit" />
                <IconDanger icon="trash" />
              </IconBar>
            </h1>
            <br />
            <a href="#__top">back to top</a>
          </td>
        </tr>
        <tr>
          <th scope="col">
            name
            <IconAction
              icon="plus"
              class="hover"
              @click="
                currentTable = table
                columnAdd = true
              "
            />
            <ColumnEditModal
              v-if="columnAdd"
              :schema="schema"
              :table="currentTable.name"
              :metadata="tables"
              @close="
                columnAdd = false
                loadSchema()
              "
            />
          </th>
          <th scope="col">type</th>
          <th scope="col">description</th>
        </tr>
        <tr v-for="column in table.columns" :key="column.name">
          <td>
            {{ column.name }}
            <IconBar class="hover">
              <IconAction
                icon="edit"
                @click="
                  currentTable = table
                  currentColumn = column
                  columnAlter = true
                "
              />
              <IconDanger
                icon="trash"
                @click="
                  currentTable = table
                  currentColumn = column
                  columnDrop = true
                "
              />
              <ColumnEditModal
                v-if="columnAlter"
                :defaultValue="currentColumn"
                :schema="schema"
                :table="currentTable.name"
                :metadata="tables"
                @close="
                  columnAlter = false
                  loadSchema()
                "
              />
              <ColumnDropModal
                v-if="columnDrop"
                :schema="schema"
                :table="currentTable.name"
                :column="currentColumn.name"
                @close="
                  columnDrop = false
                  loadSchema()
                "
              />
            </IconBar>
          </td>
          <td>
            <span>{{ column.columnType }}</span>
            <span v-if="column.refTable"
              >({{ column.refTable }}.{{ column.refColumn }})</span
            >&nbsp;
            <span v-if="column.nullable">NULLABLE</span>
          </td>
          <td>{{ column.description }}</td>
        </tr>
      </tbody>
    </table>
  </LayoutCard>
</template>

<style scoped>
h1 {
  display: inline-block;
}
th {
  font-weight: bold;
}
.hover {
  opacity: 0;
  float: right;
}
table tr:hover .hover {
  opacity: 1;
}
table th:hover .hover {
  opacity: 1;
}
</style>

<script>
import { request } from 'graphql-request'
import Vue from 'vue'
import VScrollLock from 'v-scroll-lock'
import {
  IconBar,
  IconAction,
  IconDanger,
  Spinner,
  LayoutCard,
  MessageError,
  InputBoolean
} from '@mswertz/emx2-styleguide'
import ColumnEditModal from './ColumnEditModal'
import ColumnDropModal from './ColumnDropModal'

Vue.use(VScrollLock)

export default {
  props: {
    schema: String
  },
  components: {
    IconBar,
    IconAction,
    IconDanger,
    Spinner,
    LayoutCard,
    MessageError,
    InputBoolean,
    ColumnEditModal,
    ColumnDropModal
  },
  data: function() {
    return {
      showAttributes: false,
      loading: false,
      tables: null,
      error: null,
      currentTable: null,
      currentColumn: null,
      columnAlter: false,
      columnAdd: false,
      columnDrop: false
    }
  },
  methods: {
    // alter(column) {},
    // drop(column) {},
    loadSchema() {
      this.loading = true
      request(
        this.endpoint,
        '{_meta{tables{name,pkey,description,columns{name,columnType,pkey,refTable,refColumn,nullable,description}}}}'
      )
        .then(data => (this.tables = data._meta.tables))
        .catch(error => {
          if (error.response.error.status === 403) {
            this.error = 'Forbidden. Do you need to login?'
          } else this.error = error.response.error
        })
      this.loading = false
    }
  },
  computed: {
    endpoint() {
      return '/api/graphql/' + this.schema
    },
    yuml() {
      if (!this.tables) return ''
      let res = 'http://yuml.me/diagram/scruffy;dir:lr/class/'
      // classes
      this.tables.forEach(table => {
        res += `[${table.name}`
        if (this.showAttributes) {
          res += '|'
          table.columns.forEach(column => {
            res += `${column.name};`
          })
        }
        res += `],`
      })
      // relations
      this.tables.forEach(table => {
        table.columns.forEach(column => {
          if (column.columnType === 'REF') {
            res += `[${table.name}]->[${column.refTable}],`
          } else if (column.columnType === 'REF_ARRAY') {
            res += `[${table.name}]-*>[${column.refTable}],`
          }
        })
      })
      return res
    }
  },
  created() {
    this.loadSchema()
  }
}
</script>

<docs>
Example
```
  <Schema schema="pet store" />
```
</docs>
