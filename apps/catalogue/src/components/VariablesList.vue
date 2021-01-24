<template>
  <div>
    <InputSearch v-model="search" />
    <p v-if="count == 0">No variables found</p>
    <div v-else class="mt-2">
      <Pagination class="mb-2" :count="count" :limit="limit" v-model="page" />
      <MessageError v-if="error">{{ error }}</MessageError>
      <div class="card-columns">
        <VariableCard
          v-for="variable in variables"
          :key="
            variable.dataset.collection.acronym +
            variable.dataset.name +
            variable.name
          "
          :variable="variable"
          :datasetName="datasetName"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
dt {
  float: left;
  clear: left;
  width: 100px;
  font-weight: bold;
}

dd {
  margin: 0 0 0 110px;
  padding: 0 0 0.5em 0;
}
</style>

<script>
import { request } from "graphql-request";
import {
  InputSearch,
  MessageError,
  Pagination,
} from "@mswertz/emx2-styleguide";
import HarmonisationDetails from "./HarmonisationDetails";
import VariableCard from "./VariableCard";

export default {
  components: {
    HarmonisationDetails,
    Pagination,
    MessageError,
    VariableCard,
    InputSearch,
  },
  props: {
    collectionAcronym: String,
    datasetName: String,
  },
  data() {
    return {
      variables: [],
      search: null,
      count: 0,
      error: null,
      page: 1,
      limit: 20,
    };
  },
  methods: {
    reload() {
      this.error = null;
      let filter = {};
      if (this.collectionAcronym) {
        filter.collection = { acronym: { equals: this.collectionAcronym } };
      }
      if (this.datasetName) {
        filter.dataset = { name: { equals: this.datasetName } };
      }
      if (this.search) {
        filter._search = this.search;
      }
      request(
        "graphql",
        `query Variables($filter:VariablesFilter,$offset:Int,$limit:Int){Variables(offset:$offset,limit:$limit,filter:$filter){name, dataset{name,collection{acronym}},label, format{name},unit{name}, description,harmonisations{match{name},sourceDataset{name,collection{acronym}}}}
        ,Variables_agg(filter:$filter){count}}`,
        {
          filter: filter,
          offset: (this.page - 1) * this.limit,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.variables = data.Variables;
          this.count = data.Variables_agg.count;
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  watch: {
    collectionAcronym() {
      this.reload();
    },
    datasetName() {
      this.reload();
    },
    page() {
      this.reload();
    },
    search() {
      this.reload();
    },
  },
  created() {
    this.reload();
  },
};
</script>
