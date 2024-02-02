<template>
  <h1>Catalogue root page</h1>
  {{ data }}
</template>

<script setup lang="ts">
const data = await useFetch("/catalogue/graphql", {
  server: false,
  method: "POST",
  body: {
    query: "{Cohorts {name}}",
  },
});
// const data = await useFetch("http://localhost:3000/catalogue/catalogue/graphql", {
//   server: false,
//   method: "POST",
//   body: {
//     query: `
//                 query Networks {
//                   Networks {
//                      models {
//                       id
//                      }
//                 }`,
//   },
// });
</script>
