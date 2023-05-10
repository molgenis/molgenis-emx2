<template>
  <div class="container bg-white">
    <ResourceHeader
      :resource="datasource"
      headerCss="bg-secondary text-white"
      table-name="Datasources"
    />
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div class="row">
      <div class="col-8">
        <h6>Population</h6>
        <OntologyTerms :terms="datasource.population" color="secondary" />
        <h6>Databanks</h6>
        <DatabankList :databanks="datasource.databanks" color="secondary" />
        <h6>Data use conditions</h6>
        <OntologyTerms :terms="datasource.conditions" color="secondary" />
        <p v-if="datasource.conditionsDescription">
          {{ datasource.conditionsDescription }}
        </p>
        <h6>Summary statistics<i class="fa fa-caret-down"></i></h6>
        <p v-if="datasource.statistics">{{ datasource.statistics }}</p>
      </div>
      <div class="col-4">
        <h6>Institutions with access</h6>
        <InstitutionList :institutions="datasource.institution" />
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
import { MessageError } from "molgenis-components";
import DatabankList from "../components/DatabankList.vue";
import InstitutionList from "../components/InstitutionList.vue";
import ResourceHeader from "../components/ResourceHeader.vue";
import OntologyTerms from "../components/OntologyTerms.vue";

export default {
  components: {
    OntologyTerms,
    ResourceHeader,
    MessageError,
    DatabankList,
    InstitutionList,
  },
  props: {
    pid: String,
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
        `query Datasources($pid:String){Datasources(filter:{pid:{equals:[$pid]}}){name,pid,logo{url},releases{resource{pid},version},population{name},inclusionCriteria{name}type{name},networks{pid,name}conditionsDescription,conditions{name,definition}databanks{pid,name,type{name,definition}},institution{pid,name} description,homepage}}`,
        {
          pid: this.pid,
        }
      )
        .then((data) => {
          this.datasource = data.Datasources[0];
          if (this.datasource.releases) {
            this.version =
              this.datasource.releases[
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
    pid() {
      this.reload();
    },
  },
};
</script>
