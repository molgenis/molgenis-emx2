<template>
  Validate the RDF API output for the complete schema
  <InfoPopover>
    output is deemed valid if nodes adhere to the requirements or those nodes
    are not present
  </InfoPopover>
  <MessageError v-if="error">{{ error }}</MessageError>
  <ShaclSetItem v-for="shaclSet in shaclSets" :shaclSet="shaclSet" />
</template>

<script setup lang="ts">
import { ref } from "vue";
import { parse } from "yaml";
// @ts-expect-error
import { InfoPopover, MessageError } from "molgenis-components";
import ShaclSetItem from "../components/ShaclSetItem.vue";

const shaclSets = ref(null);
const error = ref("");
async function fetchShacls() {
  const res = await fetch("/api/rdf?shacls");
  if (res.status === 200) {
    shaclSets.value = parse(await res.text());
  } else {
    error.value =
      "Could not load available SHACL sets. Please check if you have access to any schema's to validate.";
  }
}
fetchShacls();
</script>
