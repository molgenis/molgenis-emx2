<template>
    <h1>test page</h1>
    <!-- {{ data }} -->
  </template>
  
  <script setup lang="ts">
  const data = await useFetch("catalogue/graphql", {
    server: true,
    method: "POST",
    body: {
      query: "{ Cohorts { name } }",
    },
  });
  </script>
  