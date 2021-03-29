<template>
  <div class="container bg-white">
    <ResourceHeader
      :resource="network"
      headerCss="bg-primary text-white"
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
import ReleasesList from "../components/ReleasesList";
import DocumentationList from "../components/DocumentationList";
import ResourceContext from "../components/ResourceContext";

export default {
  components: {
    ResourceContext,
    DocumentationList,
    ReleasesList,
    DatabankList,
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
    acronym: String,
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
        `query Networks($acronym:String){Networks(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},institution{acronym,name}, description,homepage,funding, partners{institution{acronym,name,country{name}}}, datasources{acronym,name}, databanks{acronym,name}, releases{resource{acronym,name},version}}}`,
        {
          acronym: this.acronym,
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
    acronym() {
      this.reload();
    },
  },
};
</script>
