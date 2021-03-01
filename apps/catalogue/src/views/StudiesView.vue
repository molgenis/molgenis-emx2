<template>
  <div class="container bg-white">
    <ResourceHeader
      :resource="study"
      headerCss="bg-success text-white"
      table-name="Studies"
    />
    <MessageError v-if="error">{{ error }}</MessageError>
    <div class="row">
      <div class="col">
        <h6>Provider</h6>
        <InstitutionList :institutions="study.provider" />
        <h6>Partners</h6>
        <PartnersList :partners="study.partners" />
        <h6>Datasources involved</h6>
        <DatasourceList :datasources="study.datasources" />
        <h6>Databanks involved</h6>
        <DatabankList :databanks="study.databanks" />
        <h6>Funding</h6>
        <p>{{ study.funding ? study.funding : "N/A" }}</p>
      </div>
    </div>
  </div>
</template>
<script>
import { request } from "graphql-request";
import { MessageError, TableExplorer } from "@mswertz/emx2-styleguide";
import VariablesList from "../components/VariablesList";
import Property from "../components/Property";
import InstitutionList from "../components/InstitutionList";
import DatabankList from "../components/DatabankList";
import DatasourceList from "../components/DatasourceList";
import PartnersList from "../components/PartnersList";
import ResourceHeader from "../components/ResourceHeader";

export default {
  components: {
    DatasourceList,
    InstitutionList,
    DatabankList,
    VariablesList,
    PartnersList,
    Property,
    MessageError,
    TableExplorer,
    ResourceHeader,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      error: null,
      study: null,
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Studies($acronym:String){Studies(filter:{acronym:{equals:[$acronym]}})
        {acronym,name,partners{institution{acronym,name}},networks{acronym,name},databanks{acronym,name}}}`,
        {
          acronym: this.acronym,
        }
      )
        .then((data) => {
          this.study = data.Studies[0];
        })
        .catch((error) => {
          if (error.response) this.error = error.response.errors[0].message;
          else this.error = error;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.reload();
  },
};
</script>
