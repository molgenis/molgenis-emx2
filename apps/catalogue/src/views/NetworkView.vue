<template>
  <div class="container bg-white">
    <ResourceHeader
      :resource="network"
      headerCss="bg-primary text-white"
      table-name="Networks"
    />
    <div class="row">
      <div class="col">
        <h6 v-if="network.datasources">Datasources involved</h6>
        <DatasourceList
          v-if="network.datasources"
          :datasources="network.datasources"
        />
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
import {
  MessageError,
  ReadMore,
  InputSelect,
  NavTabs,
} from "@mswertz/emx2-styleguide";
import VariableTree from "../components/VariableTree";
import HarmonisationList from "../components/HarmonisationList";
import PublicationList from "../components/PublicationList";
import PartnersList from "../components/PartnersList";
import InstitutionList from "../components/InstitutionList";
import ResourceHeader from "../components/ResourceHeader";
import DatasourceList from "../components/DatasourceList";
import DatabankList from "../components/DatabankList";
import CohortList from "../components/CohortList";
import ReleasesList from "../components/ReleasesList";
import DocumentationList from "../components/DocumentationList";
import ResourceContext from "../components/ResourceContext";

export default {
  components: {
    ResourceContext,
    DocumentationList,
    ReleasesList,
    DatabankList,
    CohortList,
    DatasourceList,
    InstitutionList,
    PartnersList,
    PublicationList,
    HarmonisationList,
    VariableTree,
    MessageError,
    ReadMore,
    InputSelect,
    NavTabs,
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
        .then((data) => {
          this.network = data.Networks[0];
        })
        .catch((error) => {
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
