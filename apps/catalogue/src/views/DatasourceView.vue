<template>
  <div class="container bg-white">
    <ResourceHeader
      :resource="datasource"
      headerCss="bg-warning text-dark"
      table-name="Datasource"
    />
    <MessageError v-if="error">{{ error }}</MessageError>
    <div class="row">
      <div class="col">
        <h6>Population</h6>
        <OntologyTerms :terms="datasource.population" color="warning" />
        <h6>Inclusion criteria</h6>
        <OntologyTerms :terms="datasource.inclusionCriteria" color="warning" />
        <h6>Databanks</h6>
        <DatabankList :databanks="datasource.databanks" />
        <h6>Summary statistics</h6>
        <p>{{ datasource.statistics ? datasource.statistics : "N/A" }}</p>
      </div>
      <div class="col">
        <h6>Provider</h6>
        <InstitutionList :institutions="datasource.provider" />
        <h6>Contact</h6>
        <ContactList :contacts="datasource.contact" />
        <h6>Networks</h6>
        <NetworkList :networks="datasource.networks" />
        <h6>Contributors</h6>
        <p>{{ datasource.contacts ? datasource.contacts : "N/A" }}</p>
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
import ContactList from "../components/ContactList";
import OntologyTerms from "../components/OntologyTerms";

export default {
  components: {
    OntologyTerms,
    ContactList,
    NetworkList,
    ResourceHeader,
    MessageError,
    ReadMore,
    VariablesList,
    InputSelect,
    NavTabs,
    DatabankList,
    InstitutionList,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      error: null,
      datasource: {},
      version: null,
      tab: "Data",
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Datasources($acronym:String){Datasources(filter:{acronym:{equals:[$acronym]}}){name,acronym,population{name},inclusionCriteria{name}type{name},networks{acronym,name}databanks{acronym,name},provider{acronym,name} description,homepage}}`,
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
          console.log(error);

          this.error = error.response.errors[0].message;
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
