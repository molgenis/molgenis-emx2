<template>
  <div>
    <h1>
      <small>Network:</small><br />{{ network.name }} ({{ network.acronym }})
    </h1>
    <label>Description:</label>
    <ReadMore
      :text="network.description"
      length="1000"
      v-if="network.description"
    />
    <h4>Datasets:</h4>
    <DatasetList :networkAcronym="network.acronym" />
  </div>
</template>

<script>
import { request } from "graphql-request";
import { MessageError, ReadMore } from "@mswertz/emx2-styleguide";
import DatasetList from "../components/DatasetList";

export default {
  components: {
    MessageError,
    ReadMore,
    DatasetList,
  },
  props: {
    networkAcronym: String,
  },
  data() {
    return {
      error: null,
      network: {},
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Networks($acronym:String){Networks(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},provider{acronym,name}, description,website, investigators{name}, supplementaryInformation, datasets{name}}}`,
        {
          acronym: this.networkAcronym,
        }
      )
        .then((data) => {
          this.network = data.Networks[0];
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
    networkAcronym() {
      this.reload();
    },
  },
};
</script>
