<template>
    <div class="container bg-white">
        <ResourceHeader
            header-css="bg-info text-white"
            :resource="institution"
            table-name="Institutions"
        />
        <MessageError v-if="graphqlError">
            {{ graphqlError }}
        </MessageError>
        <hr class="border-info">
        <div class="row">
            <div class="col-7">
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
                <PartnerInList :partner-in="institution.partnerIn" />
            </div>
            <div class="col-5">
                <h6>Country</h6>
                <p>{{ institution.country ? institution.country.name : "N/A" }}</p>
                <h6>Contacts</h6>
                <p>{{ institution.contacts ? institution.contacts : "N/A" }}</p>
            </div>
        </div>
    </div>
</template>

<script>
import { request } from "graphql-request";
import { MessageError } from "@/components/ui/index.js";
import DatasourceList from "@/components/catalogue/DatasourceList.vue";
import DatabankList from "@/components/catalogue/DatabankList.vue";
import NetworkList from "@/components/catalogue/NetworkList.vue";
import ResourceHeader from "@/components/catalogue/ResourceHeader.vue";
import PartnerInList from "@/components/catalogue/PartnerInList.vue";

export default {
  components: {
    PartnerInList,
    ResourceHeader,
    DatabankList,
    DatasourceList,
    NetworkList,
    MessageError,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      graphqlError: null,
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
        `query Institutions($acronym:String){Institutions(filter:{acronym:{equals:[$acronym]}}){name,acronym,logo{url},country{name},description,homepage,providerOf{acronym,name,mg_tableclass,contents{name},type{name}},partnerIn{resource{acronym,name,mg_tableclass},role{name}}}}`,
        {
          acronym: this.acronym,
        }
      )
        .then((data) => {
          this.institution = data.Institutions[0];
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
