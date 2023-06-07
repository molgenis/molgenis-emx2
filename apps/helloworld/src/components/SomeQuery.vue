<template>
  <div>
    <div v-if="loading">loading...</div>
    <div v-else-if="graphqlError">Error: {{ graphqlError }}</div>
    <div v-else>result: {{ rows }}</div>
  </div>
</template>

<script setup>
import { request } from "graphql-request";
import { ref } from "vue";

const rows = ref(Array);
const loading = ref(true);
const graphqlError = ref(null);

const query = "{Pet{name}}";
request("graphql", query)
  .then(data => {
    rows.value = data["Pet"];
    loading.value = false;
  })
  .catch(error => {
    if (Array.isArray(error.response.errors)) {
      graphqlError.value = error.response.errors[0].message;
    } else {
      graphqlError.value = error;
    }
    loading.value = false;
  });
</script>
