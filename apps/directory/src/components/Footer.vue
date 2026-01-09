<template>
  <div class="text-center">
    <div>
      This database was created using the
      <a href="http://molgenis.org">MOLGENIS</a>&nbsp;
      <a href="http://github.com/molgenis/molgenis-emx2"> molgenis-emx2 </a>
      open source software (license:
      <a href="https://github.com/molgenis/molgenis-emx2/blob/master/LICENSE">
        LGPLv3
      </a>
      ).
    </div>
    <div>
      Please cite
      <a
        href="https://www.liebertpub.com/doi/10.1089/bio.2016.0088"
        target="_blank"
      >
        Holub et al. (2016)
      </a>
      on use.
    </div>
    <div v-if="session?.manifest">
      Software version:
      <a
        :href="
          'https://github.com/molgenis/molgenis-emx2/releases/tag/' +
          session?.manifest.SpecificationVersion
        "
      >
        {{ session?.manifest.SpecificationVersion }}
      </a>
      .
      <span v-if="session?.manifest.DatabaseVersion">
        Database version: {{ session?.manifest.DatabaseVersion }}.
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  session?: any;
}>();
</script>

<style scoped>
.navbar {
  background-color: white;
}
</style>
