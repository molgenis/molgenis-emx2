<template>
  <div>
    <div v-if="loading">loading...</div>
    <div v-else-if="graphqlError">Error: {{ graphqlError }}</div>
    <div v-else>result: {{ rows }}</div>
  </div>
</template>

<script>
import { request } from "graphql-request";

export default {
  data() {
    return {
      rows: Array,
      loading: false,
      graphqlError: null,
    };
  },
  created() {
    let query = "{Pet{name}}";
    this.loading = true;
    //do query
    request("graphql", query)
      .then((data) => {
        this.rows = data["Pet"];
        this.loading = false;
      })
      .catch((error) => {
        if (Array.isArray(error.response.errors)) {
          this.graphqlError = error.response.errors[0].message;
        } else {
          this.graphqlError = error;
        }
        this.loading = false;
      });
  },
};
</script>
