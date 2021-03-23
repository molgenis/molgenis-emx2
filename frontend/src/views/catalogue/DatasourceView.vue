<template>
  <div class="container bg-white">
    <ResourceHeader
      header-css="bg-warning text-dark"
      :resource="datasource"
      table-name="Datasources"
    />
    <MessageError v-if="graphqlError">
      {{ graphqlError }}
    </MessageError>
    <div class="row">
      <div class="col">
        <h6>Population</h6>
        <OntologyTerms color="warning" :terms="datasource.population" />
        <h6>Inclusion criteria</h6>
        <OntologyTerms color="warning" :terms="datasource.inclusionCriteria" />
        <h6>Databanks</h6>
        <DatabankList :databanks="datasource.databanks" />
        <h6>Summary statistics</h6>
        <p>{{ datasource.statistics ? datasource.statistics : "N/A" }}</p>
      </div>
      <div class="col">
        <ResourceContext :resource="datasource" />
      </div>
    </div>
  </div>
</template>

<script>
import DatabankList from '@/components/catalogue/DatabankList.vue'
import {MessageError} from '@/components/ui/index.js'
import OntologyTerms from '@/components/catalogue/OntologyTerms.vue'
import {request} from 'graphql-request'
import ResourceContext from '@/components/catalogue/ResourceContext.vue'
import ResourceHeader from '@/components/catalogue/ResourceHeader.vue'

export default {
  components: {
    DatabankList,
    MessageError,
    OntologyTerms,
    ResourceContext,
    ResourceHeader,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      datasource: {},
      graphqlError: null,
      tab: 'Data',
      version: null,
    }
  },
  watch: {
    databankAcronym() {
      this.reload()
    },
  },
  created() {
    this.reload()
  },
  methods: {
    reload() {
      request(
        'graphql',
        'query Datasources($acronym:String){Datasources(filter:{acronym:{equals:[$acronym]}}){name,acronym,logo{url},releases{resource{acronym},version},population{name},inclusionCriteria{name}type{name},networks{acronym,name}databanks{acronym,name},provider{acronym,name} description,homepage}}',
        {
          acronym: this.acronym,
        },
      )
        .then((data) => {
          this.datasource = data.Datasources[0]
          if (this.datasource.releases) {
            this.version = this.datasource.releases[
              this.datasource.releases.length - 1
            ].version
          }
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
