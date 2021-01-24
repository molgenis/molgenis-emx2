<template>
  <div v-if="dataset">
    <h1><small>Dataset:</small>&nbsp;{{ dataset.name }}</h1>
    <Property label="Collection">{{ dataset.collection.name }}</Property>
    <Property label="Description">{{ dataset.description }}</Property>
    <MessageError v-if="error"> {{ error }}</MessageError>
    <h4>Variables:</h4>
    <VariablesList
      :collection-acronym="collectionAcronym"
      :dataset-name="datasetName"
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
    networkAcronym: String,
    collectionAcronym: String,
    datasetName: String,
  },
  data() {
    return {
      error: null,
      dataset: null,
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Datasets($collection:ResourcesPkeyInput,$name:String){Datasets(filter:{collection:{equals:[$collection]},name:{equals:[$name]}})
        {name,collection{name},description,label,topics{name},completeness,timeline,populations{name},supplementaryInformation}}`,
        {
          collection: {
            acronym: this.collectionAcronym
              ? this.collectionAcronym
              : this.networkAcronym,
          },
          name: this.datasetName,
        }
      )
        .then((data) => {
          this.dataset = data.Datasets[0];
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
