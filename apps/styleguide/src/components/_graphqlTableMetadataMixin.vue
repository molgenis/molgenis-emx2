<template>
  <div>nothing</div>
</template>

<script>
import { request } from 'graphql-request'

export default {
  props: {
    schema: String,
    table: String
  },
  data: function() {
    return {
      metadata: {},
      loading: true,
      error: null
    }
  },
  methods: {
    reloadMetadata() {
      this.loading = true
      request(
        this.endpoint,
        '{_meta{tables{name,pkey,columns{name,columnType,pkey,refTable,refColumn,nullable}}}}'
      )
        .then(data => {
          data._meta.tables.forEach(element => {
            if (element.name === this.table) {
              this.metadata = element
            }
          })
        })
        .catch(error => (this.error = 'internal server error' + error))
      this.loading = false
    }
  },
  computed: {
    endpoint() {
      return '/api/graphql/' + this.schema
    }
  },
  watch: {
    schema: 'reloadMetadata',
    table: 'reloadMetadata'
  },
  created() {
    this.reloadMetadata()
  }
}
</script>
