<template>
  <div v-if="table">
    <h1><small>Table:</small>&nbsp;{{ table.name }}</h1>
    <Property
      :label="
        table.resource.mg_tableclass.includes('Project')
          ? 'Project'
          : 'Databank'
      "
    >
      {{ table.resource.name }}
    </Property>
    <Property label="Description">{{ table.description }}</Property>
    <MessageError v-if="error"> {{ error }}</MessageError>
    <h4>Variables:</h4>
    <VariablesList
      :resource-acronym="resourceAcronym"
      :table-name="tableName"
    />
  </div>
</template>
<script>
import { request } from "graphql-request";
import VariablesList from "../components/VariablesList";
import Property from "../components/Property";

export default {
  components: {
    VariablesList,
    Property,
  },
  props: {
    projectAcronym: String,
    resourceAcronym: String,
    tableName: String,
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
        `query Tables($resource:String,$name:String){Tables(filter:{resource:{acronym:{equals:[$resource]}},name:{equals:[$name]}})
        {name,resource{name,mg_tableclass},description,label,topics{name},populations{name}}}`,
        {
          resource: this.resourceAcronym
            ? this.resourceAcronym
            : this.projectAcronym,
          name: this.tableName,
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
