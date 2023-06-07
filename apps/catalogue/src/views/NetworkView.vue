<template>
  <div class="container bg-white">
    <ResourceHeader
      :resource="network"
      headerCss="bg-primary text-white"
      table-name="Networks" />
    <div class="row">
      <div class="col">
        <h6 v-if="network.datasources">Datasources involved</h6>
        <DatasourceList
          v-if="network.datasources"
          :datasources="network.datasources" />
        <h6>Databanks involved</h6>
        <DatabankList :databanks="network.databanks" />
        <h6>Cohorts involved</h6>
        <CohortList :cohorts="network.cohorts" />
        <h6>Funding</h6>
        <p>{{ network.fundingStatement ? network.fundingStatement : "N/A" }}</p>
      </div>
      <div class="col">
        <ResourceContext :resource="network" />
      </div>
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";
import ResourceHeader from "../components/ResourceHeader.vue";
import DatasourceList from "../components/DatasourceList.vue";
import DatabankList from "../components/DatabankList.vue";
import CohortList from "../components/CohortList.vue";
import ResourceContext from "../components/ResourceContext.vue";

export default {
  components: {
    ResourceContext,
    DatabankList,
    CohortList,
    DatasourceList,
    ResourceHeader,
  },
  props: {
    pid: String,
  },
  data() {
    return {
      version: null,
      graphqlError: null,
      network: {},
      tab: "Variables",
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Networks($pid:String){Networks(filter:{pid:{equals:[$pid]}}){name,pid,type{name},institution{pid,name}, contact{name,email},description,homepage,fundingStatement, partners{institution{pid,name,country{name}}}, datasources{pid,name}, cohorts{pid,name}, databanks{pid,name},models{pid,name}, releases{resource{pid,name},version}}}`,
        {
          pid: this.pid,
        }
      )
        .then(data => {
          this.network = data.Networks[0];
        })
        .catch(error => {
          this.graphqlError = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.reload();
  },
  watch: {
    pid() {
      this.reload();
    },
  },
};
</script>
