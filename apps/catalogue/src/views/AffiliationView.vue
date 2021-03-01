<template>
  <div class="container bg-white">
    <ResourceHeader
      header-css="bg-info text-white"
      table-name="Affiliation"
      :resource="affiliation"
    />
    <MessageError v-if="error">{{ error }}</MessageError>
    <div class="row">
      <div class="col">
        <h6>Institution</h6>
        <InstitutionList :institutions="[affiliation.institution]" />
        <h6>Partner in</h6>
        <PartnerInList :partner-in="affiliation.partnerIn" />
      </div>
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";
import { MessageError } from "@mswertz/emx2-styleguide";
import ResourceHeader from "../components/ResourceHeader";
import InstitutionList from "../components/InstitutionList";
import PartnerInList from "../components/PartnerInList";

export default {
  components: {
    PartnerInList,
    InstitutionList,
    ResourceHeader,
    MessageError,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      error: null,
      affiliation: {},
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Affiliations($acronym:String){Affiliations(filter:{acronym:{equals:[$acronym]}}){name,description,homepage,institution{name,acronym}, partnerIn{resource{acronym,name,mg_tableclass},role{name}}}}`,
        {
          acronym: this.acronym,
        }
      )
        .then((data) => {
          this.affiliation = data.Affiliations[0];
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
