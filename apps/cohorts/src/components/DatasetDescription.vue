<template>
  <div>
    <h1>Dataset: {{ datasetName }}</h1>
    <p>{{ dataset.label }}</p>
    <MessageError v-if="error">{{ error }}</MessageError>
    <DatasetTabs
      selected="description"
      :dataset-name="datasetName"
      :collection-acronym="collectionAcronym"
    />
    <div
      v-for="item in [
        'description',
        'completeness',
        'target',
        'timeline',
        'comments',
        'constraints',
      ]"
    >
      <h3>{{ item }}</h3>
      <p>{{ dataset[item] ? dataset[item] : "N/A" }}</p>
    </div>
  </div>
</template>
<script>
import { request } from "graphql-request";
import { MessageError } from "@mswertz/emx2-styleguide";
import VariablesList from "./VariablesList";
import DatasetTabs from "./DatasetTabs";

export default {
  components: {
    VariablesList,
    MessageError,
    DatasetTabs,
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
        {name,description,label,topics{name},completeness,target,timeline,population{name},comments,constraints,variables{name}}}`,
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
};
</script>
