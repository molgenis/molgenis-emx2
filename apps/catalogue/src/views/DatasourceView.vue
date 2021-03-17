<template>
  <div class="container bg-white">
    <ResourceHeader
      :resource="datasource"
      headerCss="bg-warning text-dark"
      table-name="Datasources"
    />
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div class="row">
      <div class="col-8">
        <h6>Population</h6>
        <OntologyTerms :terms="datasource.population" color="warning" />
        <h6>Databanks</h6>
        <DatabankList :databanks="datasource.databanks" color="warning" />
        <h6>Data use conditions</h6>
        <OntologyTerms :terms="datasource.conditions" color="warning" />
        <h6>Summary statistics<i class="fa fa-caret-down"></i></h6>
        <p v-if="datasource.statistics">{{ datasource.statistics }}</p>
      </div>
      <div class="col-4">
        <h6>Institutions with access</h6>
        <InstitutionList :institutions="datasource.provider" />
        <h6>Documentation</h6>
        <DocumentationList :documentation="datasource.documentation" />
        <h6 v-if="datasource.publications">Publications</h6>
        <PublicationList
          v-if="datasource.publications"
          :publications="datasource.publications"
        />
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
import VariablesList from "../components/VariablesList";
import DatabankList from "../components/DatabankList";
import InstitutionList from "../components/InstitutionList";
import ResourceHeader from "../components/ResourceHeader";
import NetworkList from "../components/NetworkList";
import OntologyTerms from "../components/OntologyTerms";
import ResourceContext from "../components/ResourceContext";

export default {
  components: {
    ResourceContext,
    OntologyTerms,
    ResourceHeader,
    MessageError,
    ReadMore,
    VariablesList,
    InputSelect,
    NavTabs,
    DatabankList,
    InstitutionList,
    NetworkList,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      graphqlError: null,
      datasource: {},
      version: null,
      tab: "Data",
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Datasources($acronym:String){Datasources(filter:{acronym:{equals:[$acronym]}}){name,acronym,logo{url},releases{resource{acronym},version},population{name},inclusionCriteria{name}type{name},networks{acronym,name}conditions{name,definition}databanks{acronym,name,type{name,definition}},provider{acronym,name} description,homepage}}`,
        {
          acronym: this.acronym,
        }
      )
        .then((data) => {
          console.log(data);
          this.datasource = data.Datasources[0];
          if (this.datasource.releases) {
            this.version = this.datasource.releases[
              this.datasource.releases.length - 1
            ].version;
          }
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
    databankAcronym() {
      this.reload();
    },
  },
};
</script>
