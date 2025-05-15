<script setup lang="ts">
import { useGqlFetch } from "../../composables/useGqlFetch";
import type { IManifestResponse, IMgError } from "../../interfaces/types";
import { useRoute } from "vue-router";

const route = useRoute();

const { data } = await $fetch(`/graphql`, {
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
