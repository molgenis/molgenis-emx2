<script setup>
const query = `{ Schemas { name description } }`;
const { data, error } = await fetchGql("apps/graphql", query);
console.log(data);
console.log(error);
const catalogueQuery = `{_schema{name}, Institutions_agg{count}, Studies_agg{count}, Cohorts_agg{count}}`;
const catalogueResp = await fetchGql(
  "catalogue/catalogue/graphql",
  catalogueQuery
);
console.log(catalogueResp);
const cohortCount = catalogueResp.data.Cohorts_agg.count;
</script>

<template>
  <div>
    <h1 class="text-center text-5xl">
      European Networks Health Data & Cohort Catalogue.
    </h1>
    <ul v-if="data.Schemas" class="pl-6">
      <li v-for="schema in data.Schemas" :key="schema.name">
        {{ schema.name }}
      </li>
    </ul>
    <div v-if="error">{{ error }}</div>
    <!-- <div>{{ catalogueResp.data }}</div> -->
    <HomePageCard title="Cohorts" :count="cohortCount"></HomePageCard>
   
  </div>
</template>