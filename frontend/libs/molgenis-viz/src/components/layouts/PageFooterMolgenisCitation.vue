<template>
  <div class="footer-meta molgenis-citation">
    <p>
      This database was created using
      <a href="https://www.molgenis.org/">MOLGENIS open source software</a>
      <span v-if="version">using version {{ version }}</span>
    </p>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { fetchData } from "../../utils/utils";

let version = ref(null);

const query = `{
  _manifest {
    DatabaseVersion
    SpecificationVersion
  }
}`;

onMounted(() => {
  Promise.resolve(fetchData(query)).then((response) => {
    const data = response.data._manifest;
    version.value = data.SpecificationVersion;
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
