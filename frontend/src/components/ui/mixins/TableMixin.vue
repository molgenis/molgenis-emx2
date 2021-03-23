<template>
  <ShowMore>
    <pre>graphqlError = {{ graphqlError }}</pre>
    <pre>graphql = {{ graphql }}</pre>
    <pre>data = {{ data }}</pre>
  </ShowMore>
</template>

<script>
import {request} from 'graphql-request'
import TableMetadataMixin from './TableMetadataMixin.vue'

export default {
  extends: TableMetadataMixin,
  props: {
    /** pass filters conform TableMixin */
    filter: {},
    /** Name of the table within graphql endpoint */
    table: String,
  },
  data: function() {
    return {
      count: 0,
      data: [],
      limit: 20,
      offset: 0,
      searchTerms: null,
    }
  },
  computed: {
    columnNames() {
      let result = ''
      if (this.tableMetadata != null) {
        this.tableMetadata.columns.forEach((col) => {
          if (
            ['REF', 'REF_ARRAY', 'REFBACK', 'MREF'].includes(col.columnType)
          ) {
            result = result + ' ' + col.name + '{' + this.refGraphql(col) + '}'
          } else if (col.columnType == 'FILE') {
            result = result + ' ' + col.name + '{id,size,extension,url}'
          } else {
            result = result + ' ' + col.name
          }
        })
      }
      return result
    },
    graphql() {
      if (this.tableMetadata == undefined) {
        return ''
      }
      let search =
        this.searchTerms != null && this.searchTerms !== ''
          ? ',search:"' + this.searchTerms + '"'
          : ''
      return `query ${this.table}($filter:${this.table}Filter){
              ${this.table}(filter:$filter,limit:${this.limit},offset:${this.offset}${search}){${this.columnNames}}
              ${this.table}_agg(filter:$filter${search}){count}}`
    },
    // filter can be passed as prop or overridden in subclass
    graphqlFilter() {
      if (this.filter) {
        return this.filter
      } else return {}
    },
    tableMetadata() {
      return this.getTable(this.table)
    },
  },
  watch: {
    graphqlFilter: {
      deep: true,
      handler() {
        this.reload()
      },
    },
    limit: 'reload',
    schema: 'reload',
    searchTerms: 'reload',
    table: 'reload',
  },
  methods: {
    getPkey(row) {
      let result = {}
      if (this.tableMetadata != null) {
        this.tableMetadata.columns.forEach((col) => {
          if (col.key == 1) {
            result[col.name] = row[col.name]
          }
        })
      }
      return result
    },
    getTable(table) {
      let result = undefined
      if (this.schema != null && this.schema.tables != null) {
        this.schema.tables.forEach((t) => {
          if (t.name == table) {
            result = t
          }
        })
        if (!result) {
          this.graphqlError = 'Table ' + table + ' not found'
        }
      }
      if (result) return result
    },
    refGraphql(column) {
      let graphqlString = ''
      this.getTable(column.refTable).columns.forEach((c) => {
        if (c.key == 1) {
          graphqlString += c.name + ' '
          if (['REF', 'REF_ARRAY', 'REFBACK', 'MREF'].includes(c.columnType)) {
            graphqlString += '{' + this.refGraphql(c) + '}'
          }
        }
      })
      return graphqlString
    },
    reload() {
      if (this.tableMetadata != undefined) {
        this.loading = true
        this.graphqlError = null
        request(this.graphqlURL, this.graphql, {filter: this.graphqlFilter})
          .then((data) => {
            this.data = data[this.table]
            this.count = data[this.table + '_agg']['count']
            this.loading = false
          })
          .catch((error) => {
            this.graphqlError = 'internal server graphqlError' + error
            this.loading = false
          })
      }
    },
  },
}
</script>
