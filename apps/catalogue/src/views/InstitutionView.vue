<template>
  <div class="container bg-white">
    <ResourceHeader
      :resource="institution"
      header-css="bg-info text-white"
      table-name="Institution"
    />
    <MessageError v-if="error">{{ error }}</MessageError>
    <hr class="border-info" />
    <div class="row">
      <div class="col">
        <h6>Country</h6>
        <p>{{ institution.country ? institution.country.name : "N/A" }}</p>
        <h6>Contacts</h6>
        <p>{{ institution.contacts ? institution.contacts : "N/A" }}</p>
      </div>
      <div class="col">
        <h5>Provider of:</h5>
        <div class="m-4">
          <h6>Datasources</h6>
          <DatasourceList :datasources="datasources" />
          <h6>Databanks</h6>
          <DatabankList :databanks="databanks" />
          <h6>Networks</h6>
          <NetworkList :networks="networks" />
        </div>
        <h5>Partner in:</h5>
        <PartnerInList :partnerIn="institution.partnerIn" />
      </div>
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";
import { MessageError, ReadMore, TableSearch } from "@mswertz/emx2-styleguide";
import DatasourceList from "../components/DatasourceList";
import DatabankList from "../components/DatabankList";
import NetworkList from "../components/NetworkList";
import ResourceHeader from "../components/ResourceHeader";
import PartnerInList from "../components/PartnerInList";
import OntologyTerms from "../components/OntologyTerms";

export default {
  components: {
    OntologyTerms,
    PartnerInList,
    ResourceHeader,
    DatabankList,
    DatasourceList,
    NetworkList,
    MessageError,
    ReadMore,
    TableSearch,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      error: null,
      institution: {},
    };
  },
  computed: {
    databanks() {
      let result = null;
      if (this.institution.providerOf) {
        result = this.institution.providerOf.filter((r) =>
          r.mg_tableclass.includes("Databanks")
        );
      }
      if (result && result.length > 0) {
        return result;
      }
      return null;
    },
    datasources() {
      let result = null;
      if (this.institution.providerOf) {
        result = this.institution.providerOf.filter((r) =>
          r.mg_tableclass.includes("Datasources")
        );
      }
      if (result && result.length > 0) {
        return result;
      }
      return null;
    },
    networks() {
      let result = null;
      if (this.institution.providerOf) {
        result = this.institution.providerOf.filter((r) =>
          r.mg_tableclass.includes("Network")
        );
      }
      if (result && result.length > 0) {
        return result;
      }
      return null;
    },
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Institutions($acronym:String){Institutions(filter:{acronym:{equals:[$acronym]}}){name,acronym,logo{url},country{name},description,homepage,providerOf{acronym,name,mg_tableclass},partnerIn{resource{acronym,name,mg_tableclass},role{name}}}}`,
        {
          acronym: this.acronym,
        }
      )
        .then((data) => {
          this.institution = data.Institutions[0];
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
  watch: {
    acronym() {
      this.reload();
    },
  },
};
</script>
