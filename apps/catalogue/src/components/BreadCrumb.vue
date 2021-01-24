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
            name: 'provider',
            params: { providerAcronym: this.providerAcronym },
          }"
        >
          {{ providerAcronym }}
        </RouterLink>
      </li>
      <li
        v-if="networkAcronym"
        class="breadcrumb-item active"
        aria-current="page"
      >
        <RouterLink
          :to="{
            name: networkview,
            params: { networkAcronym: this.networkAcronym },
          }"
        >
          {{ networkAcronym }}
        </RouterLink>
      </li>
      <li
        v-else-if="collectionAcronym"
        class="breadcrumb-item active"
        aria-current="page"
      >
        <RouterLink
          :to="{
            name: collectionview,
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
    </ol>
  </nav>
</template>
<script>
export default {
  props: {
    collectionAcronym: String,
    networkAcronym: String,
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
        if (this.$route.name.startsWith("variable")) {
          return "variables";
        }
      }
    },
    collectionview() {
      if (this.$route.name.startsWith("provider")) {
        return "provider-collection";
      }
      if (this.$route.name.startsWith("dataset")) {
        return "dataset-collection";
      }
      return "collection";
    },
    networkview() {
      if (this.$route.name.startsWith("dataset")) {
        return "dataset-network";
      }
      return "network";
    },
    datasetview() {
      if (this.$route.name.startsWith("collection")) {
        return "collection-dataset";
      }
      if (this.$route.name.startsWith("network")) {
        return "network-dataset";
      }
      if (this.$route.name.startsWith("provider")) {
        return "provider-dataset";
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
