<template>
  <div class="container bg-white">
    <ResourceHeader
      header-css="bg-warning text-dark"
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
import { MessageError } from "molgenis-components";
import DatabankList from "../components/DatabankList";
import NetworkList from "../components/NetworkList";
import ResourceHeader from "../components/ResourceHeader";
import ResourceContext from "../components/ResourceContext";

export default {
  components: {
    NetworkList,
    DatabankList,
    MessageError,
    ResourceHeader,
    ResourceContext,
  },
  props: {
    pid: String,
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
        `query Models($pid:String){Models(filter:{pid:{equals:[$pid]}}){contact{name,email},institution{name,pid},name,pid,type{name},networks{name,pid},institution{pid,name}, description,homepage, partners{institution{pid,name,country{name}}},,releases{resource{pid,name},version}}}`,
        {
          pid: this.pid,
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
    pid() {
      this.reload();
    },
  },
};
</script>
