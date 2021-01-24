<template>
  <nav aria-label="breadcrumb">
    <ol class="breadcrumb">
      <li class="breadcrumb-item">
        <RouterLink to="/">Catalogue</RouterLink>
      </li>
      <!-- to show the main view we are in -->
      <li v-if="mainview" class="breadcrumb-item active">
        <RouterLink
          :to="{
            name: mainview,
          }"
        >
          {{ sanitize(mainview) }}
        </RouterLink>
      </li>
      <li
        v-if="providerAcronym"
        class="breadcrumb-item active"
        aria-current="page"
      >
        <RouterLink
          :to="{
            name: 'provider-details',
            params: { providerAcronym: this.providerAcronym },
          }"
        >
          {{ providerAcronym }}
        </RouterLink>
      </li>
      <li
        v-if="collectionAcronym"
        class="breadcrumb-item active"
        aria-current="page"
      >
        <RouterLink
          :to="{
            name: 'collection',
            params: { collectionAcronym: this.collectionAcronym },
          }"
        >
          {{ collectionAcronym }}
        </RouterLink>
      </li>
      <li v-if="datasetName" class="breadcrumb-item active" aria-current="page">
        <RouterLink
          :to="{
            name: datasetview,
            params: {
              collectionAcronym: this.collectionAcronym,
              datasetName: this.datasetName,
            },
          }"
        >
          {{ datasetName }}
        </RouterLink>
      </li>
      <li
        v-if="mainview == 'variables'"
        class="breadcrumb-item active"
        aria-current="page"
      >
        <RouterLink
          :to="{
            name: 'variables',
            params: {
              collectionAcronym,
              datasetName,
            },
          }"
        >
          Variables
        </RouterLink>
      </li>
    </ol>
  </nav>
</template>
<script>
export default {
  props: {
    collectionAcronym: String,
    providerAcronym: String,
    datasetName: String,
  },
  computed: {
    mainview() {
      if (this.$route.name) {
        if (this.$route.name.startsWith("provider")) {
          return "providers";
        }
        if (this.$route.name.startsWith("network")) {
          return "networks";
        }
        if (this.$route.name.startsWith("collection")) {
          return "collections";
        }
        if (this.$route.name.startsWith("dataset")) {
          return "datasets";
        }
      }
    },
    datasetview() {
      if (this.$route.name.startsWith("collection")) {
        return "collection-dataset";
      }
      return "dataset";
    },
  },
  methods: {
    sanitize(val) {
      return val.charAt(0).toUpperCase() + val.slice(1);
    },
  },
};
</script>
