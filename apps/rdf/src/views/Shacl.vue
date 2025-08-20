<template>
  Validate the RDF API output for the complete schema
  <InfoPopover
    >output is deemed valid if nodes adhere to the requirements or those nodes
    are not present</InfoPopover
  >
  <ShaclSetItem v-for="shaclSet in shaclSets" :shaclSet="shaclSet" />
</template>

<script setup lang="ts">
import { ref } from "vue";
import { parse } from "yaml";
import { InfoPopover } from "molgenis-components";
import ShaclSetItem from "../components/ShaclSetItem.vue";

const shaclSets = ref(null);
async function fetchShacls() {
  const res = await fetch("/api/rdf?shacls");
  shaclSets.value = parse(await res.text());
}
fetchShacls();
</script>
