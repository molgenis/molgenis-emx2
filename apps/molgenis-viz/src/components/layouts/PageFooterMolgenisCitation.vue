<template>
  <div class="footer-meta molgenis-citation">
    <p>
      This database was created using
      <a href="https://www.molgenis.org/">MOLGENIS open source software</a>
      <span v-if="manifest.SpecificationVersion">
        using version {{ manifest.SpecificationVersion }}</span
      >
    </p>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { request, gql } from "graphql-request";

let manifest = ref({});

async function getManifest() {
  const query = gql`
    {
      _manifest {
        ImplementationVersion
        SpecificationVersion
        DatabaseVersion
      }
    }
  `;
  const result = await request("/api/graphql", query);
  return result._manifest;
}

async function loadData() {
  manifest.value = await getManifest();
}

onMounted(() => {
  loadData().catch((err) => {
    console.error(err);
    throw new Error("Unable to retrieve manifest");
  });
});
</script>

<style lang="scss">
.molgenis-citation {
  background-color: $gray-000;
  text-align: center;
  font-size: 10pt;
  max-width: $max-width;
  margin: 0 auto;
  padding: 0.8em;
}
</style>
