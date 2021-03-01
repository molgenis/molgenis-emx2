<template>
  <div v-if="error">
    <MessageError>{{ error }}</MessageError>
  </div>
  <div v-else-if="table" class="container bg-white">
    <div class="p-2 bg-dark text-white mb-3">
      <h6>
        <RouterLink to="/" class="text-white"> home</RouterLink>
        /
        <RouterLink to="/list/Tables" class="text-white"> tables</RouterLink>
        /
      </h6>
    </div>
    <div>
      <h1><small>Table:</small><br />&nbsp;{{ table.name }}</h1>
      <p>{{ table.description ? table.description : "N/A" }}</p>
      <h6>Release</h6>
      <RouterLink
        :to="{
          name: 'release',
          params: { acronym: acronym, version: version },
        }"
        >{{ table.release.resource.acronym }} {{ table.release.version }}
      </RouterLink>
      <MessageError v-if="error"> {{ error }}</MessageError>
      <TableExplorer
        table="Variables"
        :filter="{
          name: { equals: name },
          release: {
            version: { equals: version },
            resource: { acronym: { equals: acronym } },
          },
        }"
      />
    </div>
  </div>
</template>
<script>
import { request } from "graphql-request";
import { MessageError, TableExplorer } from "@mswertz/emx2-styleguide";
import VariablesList from "../components/VariablesList";
import Property from "../components/Property";

export default {
  components: {
    VariablesList,
    Property,
    MessageError,
    TableExplorer,
  },
  props: {
    acronym: String,
    version: String,
    name: String,
  },
  data() {
    return {
      error: null,
      table: null,
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Tables($acronym:String,$version:String,$name:String){Tables(filter:{release:{version:{equals:[$version]},resource:{acronym:{equals:[$acronym]}}},name:{equals:[$name]}})
        {name,release{version,resource{acronym,name,mg_tableclass}},description,label,topics{name}}}`,
        {
          acronym: this.acronym,
          version: this.version,
          name: this.name,
        }
      )
        .then((data) => {
          this.table = data.Tables[0];
        })
        .catch((error) => {
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
};
</script>
