<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <div v-else style="text-align: center">
      <InputSearch v-if="table" v-model="searchTerms" />
      <Pagination v-model="page" :limit="limit" :count="count" />
      <Spinner v-if="loading" />
      <DataTable
        v-else
        v-model="selectedItems"
        :columns="columns"
        :rows="rows"
        :selectColumn="selectColumn"
        :defaultValue="defaultValue"
        @select="select"
        @deselect="deselect"
      >
        <template v-slot:colheader>
          <slot name="colheader" />
        </template>
        <template v-slot:rowheader="slotProps">
          <slot name="rowheader" :row="slotProps.row" :metadata="metadata" />
        </template>
      </DataTable>
    </div>
  </div>
</template>

<script>
import _graphqlTableMixin from './_graphqlTableMixin'
import DataTable from './DataTable'
import MessageError from './MessageError'
import InputSearch from './InputSearch'
import Pagination from './Pagination.vue'
import Spinner from './Spinner.vue'

export default {
  extends: _graphqlTableMixin,
  props: {
    defaultValue: Array,
    selectColumn: String
  },
  components: {
    DataTable,
    MessageError,
    InputSearch,
    Pagination,
    Spinner
  },
  data: function() {
    return {
      selectedItems: [],
      page: 1,
      loading: true
    }
  },
  methods: {
    select(value) {
      this.$emit('select', value)
    },
    deselect(value) {
      this.$emit('deselect', value)
    }
  },
  watch: {
    selectedItems() {
      this.$emit('input', this.selectedItems)
    },
    page() {
      this.loading = true
      this.offset = this.limit * (this.page - 1)
      this.reload()
    }
  },
  computed: {
    columns() {
      if (this.metadata && this.metadata.columns) {
        return this.metadata.columns.map(col => col.name)
      }
      return null
    },
    rows() {
      if (this.data != null) {
        return this.data.map(row => {
          let result = { ...row }
          this.metadata.columns.forEach(col => {
            if (row[col.name]) {
              if (col.columnType === 'REF') {
                result[col.name] = row[col.name][col.refColumn]
              } else if (
                col.columnType === 'REF_ARRAY' ||
                col.columnType === 'REFBACK'
              ) {
                result[col.name] = row[col.name].map(val => val[col.refColumn])
              }
            }
          })
          return result
        })
      }
      return this.data
    }
  }
}
</script>

<docs>
    Example:
    ```
    <TableSearch schema="pet%20store" table="Pet" pkey="name">
        <template v-model="selectedItems" v-slot:rowheader="props">my row action {{props.row.name}}</template>
    </TableSearch>

    ```
    Example with select and default value
    ```
    <template>
        <div>
            <TableSearch
                    v-model="selectedItems"
                    schema="pet%20store"
                    table="Pet"
                    pkey="name"
                    selectColumn="name"
                    defaultValue="pooky"
            >
                <template v-slot:rowheader="props">my row action {{props.row.name}}</template>
            </TableSearch>
            Selected: {{selectedItems}}
        </div>
    </template>

    <script>
        export default {
            data: function () {
                return {
                    selectedItems: []
                };
            }
        };
    </script>
    ```
</docs>
