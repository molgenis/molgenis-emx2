<template>
  {{ shaclSets }}
</template>

<script setup lang="ts">
import { ref } from "vue";

// todo: use correct implementation (that uses dev-proxy?)
const shaclSets = ref("");
async function fetchShacls() {
  const res = await fetch("/api/rdf?shacls");
  shaclSets.value = await res.text();
  console.log(res.text());
}

fetchShacls();
</script>
