<template>
    <h1>test schema page</h1>
    {{ data }}
  </template>
  
  <script setup lang="ts">
  const route = useRoute();
  console.log(route.params.schema);
  const data = await useFetch(`/${route.params.schema}/graphql`, {
    server: true,
    method: "POST",
    body: {
      query: "{ Cohorts { name } }",
    },
  });
  </script>
  