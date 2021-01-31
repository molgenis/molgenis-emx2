<template>
  <div v-if="table">
    <h1><small>Table:</small>&nbsp;{{ table.name }}</h1>
    <Property
      :label="
        table.collection.mg_tableclass.includes('Consort')
          ? 'Consortium'
          : 'Databank'
      "
    >
      {{ table.collection.name }}
    </Property>
    <Property label="Description">{{ table.description }}</Property>
    <MessageError v-if="error"> {{ error }}</MessageError>
    <h4>Variables:</h4>
    <VariablesList
      :collection-acronym="collectionAcronym"
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
    consortiumAcronym: String,
    datasetAcronym: String,
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
        `query Tables($collection:CollectionsPkeyInput,$name:String){Tables(filter:{collection:{equals:[$collection]},name:{equals:[$name]}})
        {name,collection{name,mg_tableclass},description,label,topics{name},completeness,timeline,populations{name},supplementaryInformation}}`,
        {
          collection: {
            acronym: this.databankAcronym
              ? this.databankAcronym
              : this.consortiumAcronym,
          },
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
