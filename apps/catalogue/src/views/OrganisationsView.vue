<template>
  <div class="container bg-white">
    <ResourceHeader
      :resource="organisation"
      header-css="bg-dark text-white"
      table-name="Organisations"
    />
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <hr class="border-dark" />
    <div class="row">
      <div class="col-7">
        <h5>Provider of:</h5>
        <div class="m-4">
          <h6>Datasources</h6>
          <DatasourceList :datasources="datasources" color="dark" />
          <h6>Networks</h6>
          <NetworkList :networks="networks" color="dark" />
        </div>
        <h5>Partner in:</h5>
        <PartnerInList :partnerIn="institution.partnerIn" />
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
import { MessageError } from "molgenis-components";
import DatasourceList from "../components/DatasourceList.vue";
import DatabankList from "../components/DatabankList.vue";
import NetworkList from "../components/NetworkList.vue";
import ResourceHeader from "../components/ResourceHeader.vue";
import PartnerInList from "../components/PartnerInList.vue";

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
    id: String,
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
  methods: {
    reload() {
      request(
        "graphql",
        `query Organisations($id:String){Organisations(filter:{id:{equals:[$id]}}){name,id,pid,logo{url},country{name},description,homepage,providerOf{id,pid,name,mg_tableclass,keywords,type{name}},partnerIn{resource{id,pid,name,mg_tableclass},role{name}}}}`,
        {
          id: this.id,
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
  created() {
    this.reload();
  },
  watch: {
    id() {
      this.reload();
    },
  },
};
</script>
