<script setup>
const query = `{ Schemas { name description } }`;
const { data, error } = await fetchGql("/apps/graphql", query);
console.log(data);
console.log(error);
</script>

<template>
  <div>
    <h1>Hello catalogue</h1>
    <div>{{ data }}</div>
  </div>
</template>