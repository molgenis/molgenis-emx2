<template>
  <div v-for="shaclSet in shaclSets">
    <ShaclSetItem :id="shaclSet.name" :title="shaclSetItemTitle(shaclSet)">
      Sources:
      <ul><li v-for="source in shaclSet.sources"><a :href="source" target="_blank">{{source}}</a></li></ul>
      <LayoutCard title="SHACL output" />
    </ShaclSetItem>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { parse } from "yaml";
import { LayoutCard } from "molgenis-components";
import ShaclSetItem from "../components/ShaclSetItem.vue";

const shaclSets = ref("");
async function fetchShacls() {
  const res = await fetch("/api/rdf?shacls");
  shaclSets.value = await res.text();
  shaclSets.value = parse(shaclSets.value);
}

function shaclSetItemTitle(shaclSet) {
  return shaclSet.description + " (version: " + shaclSet.version + ")";
}

fetchShacls();
</script>