<script setup lang="ts">
const { data } = await $fetch("/api/graphql", {
  key: `manifest`,
  method: "POST",
  body: {
    query: ` 
    query manifest {
      _manifest {
        ImplementationVersion
        SpecificationVersion
        DatabaseVersion
      }
    }`,
  },
});
</script>

<template>
  <div class="mb-0 text-center lg:pb-5 text-title text-body-lg">
    <span v-if="data">
      Software version: {{ data?._manifest.SpecificationVersion }}
    </span>
  </div>
</template>
