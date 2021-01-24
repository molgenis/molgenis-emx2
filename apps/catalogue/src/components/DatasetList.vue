<template>
  <div>
    <Pagination class="mt-2" :count="count" :limit="limit" v-model="page" />
    <MessageError v-if="error">{{ error }}</MessageError>
    <div class="card-columns mt-2">
      <DatasetCard
        v-for="dataset in datasets"
        :dataset="dataset"
        :collectionAcronym="collectionAcronym"
      />
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
import { MessageError, Pagination } from "@mswertz/emx2-styleguide";
import DatasetCard from "../components/DatasetCard";

export default {
  components: {
    DatasetCard,
    Pagination,
    MessageError,
  },
  props: {
    collectionAcronym: String,
  },
  data() {
    return {
      datasets: [],
      count: 0,
      error: null,
      page: 1,
      limit: 9,
    };
  },
  methods: {
    reload() {
      let filter = {};
      if (this.collectionAcronym) {
        filter.collection = { acronym: { equals: this.collectionAcronym } };
      }
      request(
        "graphql",
        `query Datasets($filter:DatasetsFilter,$offset:Int,$limit:Int){Datasets(offset:$offset,limit:$limit,filter:$filter){name,collection{acronym},label,variables_agg{count}}
        ,Datasets_agg(filter:$filter){count}}`,
        {
          filter: filter,
          offset: (this.page - 1) * 10,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.datasets = data.Datasets;
          this.count = data.Datasets_agg.count;
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
    page() {
      this.reload();
    },
  },
  created() {
    this.reload();
  },
};
</script>
