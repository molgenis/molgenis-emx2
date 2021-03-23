<template>
  <div class="container bg-white">
    <ResourceHeader
      header-css="bg-primary text-white"
      :resource="network"
      table-name="Networks"
    />
    <div class="row">
      <div class="col">
        <h6>Datasources involved</h6>
        <DatasourceList :datasources="network.datasources" />
        <h6>Databanks involved</h6>
        <DatabankList :databanks="network.databanks" />
        <h6>Funding</h6>
        <p>{{ network.funding ? network.funding : "N/A" }}</p>
      </div>
      <div class="col">
        <ResourceContext :resource="network" />
      </div>
    </div>
  </div>
</template>

<script>
import DatabankList from '@/components/catalogue/DatabankList.vue'
import DatasourceList from '@/components/catalogue/DatasourceList.vue'
import {request} from 'graphql-request'
import ResourceContext from '@/components/catalogue/ResourceContext.vue'
import ResourceHeader from '@/components/catalogue/ResourceHeader.vue'

export default {
  components: {
    DatabankList,
    DatasourceList,
    ResourceContext,
    ResourceHeader,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      graphqlError: null,
      network: {},
      tab: 'Variables',
      version: null,
    }
  },
  watch: {
    acronym() {
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
        'query Networks($acronym:String){Networks(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},provider{acronym,name}, description,homepage,funding, partners{institution{acronym,name,country{name}}}, datasources{acronym,name}, databanks{acronym,name}, releases{resource{acronym,name},version}}}',
        {
          acronym: this.acronym,
        },
      )
        .then((data) => {
          this.network = data.Networks[0]
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
