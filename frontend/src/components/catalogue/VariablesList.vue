<template>
  <div>
    {{ topic }}
    <InputSearch v-model="search" />
    <p v-if="count == 0">
      No variables found
    </p>
    <div v-else class="mt-2">
      <Pagination
        v-model="page" class="mb-2"
        :count="count"
        :limit="limit"
      />
      <MessageError v-if="graphqlError">
        {{ graphqlError }}
      </MessageError>
      <div class="card-columns">
        <VariableCard
          v-for="variable in variables"
          :key="
            variable.release.resource.acronym +
              variable.table.name +
              variable.name
          "
          :table-name="tableName"
          :variable="variable"
        />
      </div>
    </div>
    <ShowMore title="debug">
      {{ variables }}
    </ShowMore>
  </div>
</template>

<script>
import {request} from 'graphql-request'
import VariableCard from './VariableCard'
import {InputSearch, MessageError, Pagination, ShowMore} from '@/components/ui/index.js'

export default {
  components: {
    InputSearch,
    MessageError,
    Pagination,
    ShowMore,
    VariableCard,
  },
  props: {
    resourceAcronym: String,
    tableName: String,
    topic: String,
    version: String,
  },
  data() {
    return {
      count: 0,
      graphqlError: null,
      limit: 20,
      page: 1,
      search: null,
      variables: [],
    }
  },
  watch: {
    page() {
      this.reload()
    },
    resourceAcronym() {
      this.reload()
    },
    search() {
      this.reload()
    },
    tableName() {
      this.reload()
    },
    topic() {
      this.reload()
    },
    version() {
      this.reload()
    },
  },
  created() {
    this.reload()
  },
  methods: {
    reload() {
      this.graphqlError = null
      let filter = {}
      if (this.resourceAcronym) {
        filter.release = {
          resource: {acronym: {equals: this.resourceAcronym}},
        }
      }
      if (this.tableName) {
        filter.table = {name: {equals: this.tableName}}
      }
      if (this.search) {
        filter._search = this.search
      }
      if (this.topic) {
        filter.topics = {name: {equals: this.topic}}
      }
      request(
        'graphql',
        `query Variables($filter:VariablesFilter,$offset:Int,$limit:Int){Variables(offset:$offset,limit:$limit,filter:$filter){name, release{resource{acronym,mg_tableclass},version},table{name},label, format{name},unit{name}, description,topics{name},categories{label,value,isMissing},harmonisations{match{name},sourceRelease{resource{acronym},version},targetRelease{resource{acronym},version}sourceTable{name,release{resource{acronym},version}}}}
        ,Variables_agg(filter:$filter){count}}`,
        {
          filter: filter,
          limit: this.limit,
          offset: (this.page - 1) * this.limit,
        },
      )
        .then((data) => {
          this.variables = data.Variables
          this.count = data.Variables_agg.count
          this.$forceUpdate()
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message
        })
        .finally(() => {
          this.loading = false
        })
    },
  },
}
</script>

<style scoped>
dt {
  clear: left;
  float: left;
  font-weight: bold;
  width: 100px;
}

dd {
  margin: 0 0 0 110px;
  padding: 0 0 0.5em 0;
}
</style>

