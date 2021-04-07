<template>
  <div class="container bg-white">
    <ResourceHeader
      header-css="bg-secondary text-white"
      table-name="Models"
      :resource="model"
    />
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div class="row">
      <div class="col">
        <h6>Networks involved</h6>
        <NetworkList :networks="model.networks" />
        <h6 v-if="model.databanks">Databanks involved (see networks)</h6>
        <DatabankList v-if="model.databanks" :databanks="model.databanks" />
      </div>
      <div class="col">
        <ResourceContext :resource="model" />
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
import HarmonisationList from "../components/HarmonisationList";
import DatabankList from "../components/DatabankList";
import NetworkList from "../components/NetworkList";
import InstitutionList from "../components/InstitutionList";
import PartnersList from "../components/PartnersList";
import ResourceHeader from "../components/ResourceHeader";
import ReleasesList from "../components/ReleasesList";
import ResourceContext from "../components/ResourceContext";

export default {
  components: {
    ReleasesList,
    PartnersList,
    InstitutionList,
    NetworkList,
    DatabankList,
    HarmonisationList,
    MessageError,
    ReadMore,
    InputSelect,
    NavTabs,
    ResourceHeader,
    ResourceContext,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      version: null,
      graphqlError: null,
      model: {},
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Models($acronym:String){Models(filter:{acronym:{equals:[$acronym]}}){contact{name,email},institution{name,acronym},name,acronym,type{name},networks{name,acronym},institution{acronym,name}, description,homepage, partners{institution{acronym,name,country{name}}},,releases{resource{acronym,name},version}}}`,
        {
          acronym: this.acronym,
        }
      )
        .then((data) => {
          this.model = data.Models[0];
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
    modelAcronym() {
      this.reload();
    },
  },
};
</script>
