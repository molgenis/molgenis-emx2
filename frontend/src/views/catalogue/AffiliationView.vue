<template>
    <div class="container bg-white">
        <ResourceHeader
            header-css="bg-info text-white"
            :resource="affiliation"
            table-name="Affiliations"
        />
        <MessageError v-if="graphqlError">
            {{ graphqlError }}
        </MessageError>
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
import { request } from "graphql-request"
import { MessageError } from "@/components/ui/index.js"

import ResourceHeader from "@/components/catalogue/ResourceHeader.vue"
import InstitutionList from "@/components/catalogue/InstitutionList.vue"
import PartnerInList from "@/components/catalogue/PartnerInList.vue"

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
      graphqlError: null,
      affiliation: {},
    };
  },
  watch: {
    acronym() {
      this.reload();
    },
  },
  created() {
    this.reload();
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
          if (error.response)
            this.graphqlError = error.response.errors[0].message;
          else this.graphqlError = error;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
};
</script>
