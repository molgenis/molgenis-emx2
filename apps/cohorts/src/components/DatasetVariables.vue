<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <h1>Dataset: {{ datasetName }}</h1>
    <p>{{ dataset.label }}</p>
    <DatasetTabs
      selected="variables"
      :dataset-name="datasetName"
      :collection-acronym="collectionAcronym"
    />
    <VariablesList
      class="mt-2"
      :collection-acronym="collectionAcronym"
      :dataset-name="datasetName"
    />
  </div>
</template>
<script>
import { request } from "graphql-request";
import { MessageError } from "@mswertz/emx2-styleguide";
import VariablesList from "./VariablesList";
import DatasetTabs from "./DatasetTabs";

export default {
  components: {
    DatasetTabs,
    VariablesList,
    MessageError,
  },
  props: {
    collectionAcronym: String,
    datasetName: String,
  },
  data() {
    return {
      error: null,
      dataset: {},
    };
  },
  methods: {
    reload() {
      console.log("collections reload");
      request(
        "graphql",
        `query Datasets($collection:CollectionsPkeyInput,$name:String){Datasets(filter:{collection:{equals:[$collection]},name:{equals:[$name]}})
        {name,description,label,topics{name},completeness,timeline,populations{name},supplementaryInformation,variables{name}}}`,
        {
          collection: { acronym: this.collectionAcronym },
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
  watch: {
    collectionAcronym() {
      this.reload();
    },
    datasetName() {
      this.reload();
    },
  },
};
</script>
