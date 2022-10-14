<script setup>
const query = `{ Schemas { name description } }`;
const { data, error } = await fetchGql("/apps/graphql", query);
console.log(data);
console.log(error);
</script>

<template>
  <div>
    <h1 class="font-semibold">Schemas:</h1>
    <ul v-if="data.Schemas" class="pl-6">
      <li v-for="schema in data.Schemas" :key="schema.name">
        {{ schema.name }}
      </li>
    </ul>
    <div v-if="error">{{ error }}</div>
  </div>
</template>