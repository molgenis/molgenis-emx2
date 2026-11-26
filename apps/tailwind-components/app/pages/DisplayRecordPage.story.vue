<template>
  <p class="pb-3 text-title">
    Record pages built from the catalogue-demo schema's metadata, enhanced with
    role and display in <code>app/demoTableConfigs.ts</code>.
  </p>
  <ul class="list-disc pl-5 text-title">
    <li v-for="link in links" :key="link.to">
      <NuxtLink class="hover:underline" :to="link.to">{{
        link.label
      }}</NuxtLink>
    </li>
  </ul>
</template>

<script setup lang="ts">
const base = "/display/record/catalogue-demo/Collections/testCohort1";

const links = [
  { label: "Collection testCohort1", to: base },
  {
    label: "Subpopulation test subcohort 1A",
    to: `${base}/subpopulations/test%20subcohort%201A`,
  },
  {
    label: "Collection event test collection event 1A",
    to: `${base}/collection-events/test%20collection%20event%201A`,
  },
];
</script>
