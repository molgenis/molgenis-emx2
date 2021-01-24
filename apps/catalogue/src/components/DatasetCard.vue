<template>
  <div class="card">
    <div class="card-body">
      <h5 class="card-title">
        <RouterLink
          :to="{
            name: datasetView,
            params: {
              collectionAcronym: dataset.collection.acronym,
              networkAcronym: networkAcronym,
              datasetName: dataset.name,
              providerAcronym: providerAcronym,
            },
          }"
        >
          {{ dataset.name }}
        </RouterLink>
      </h5>
      <div v-if="!collectionAcronym && !networkAcronym">
        <p
          class="cart-text"
          v-if="dataset.collection.mg_tableclass.includes('Collection')"
        >
          <label>Collection:</label>
          <RouterLink
            :to="{
              name: 'dataset-collection',
              params: { collectionAcronym: dataset.collection.acronym },
            }"
          >
            {{ dataset.collection.acronym }}
          </RouterLink>
        </p>
        <p class="cart-text" v-else>
          <label>Network:</label>
          <RouterLink
            :to="{
              name: 'dataset-network',
              params: { networkAcronym: dataset.collection.acronym },
            }"
          >
            {{ dataset.collection.acronym }}
          </RouterLink>
        </p>
      </div>
      <p class="cart-text">
        <i>{{ dataset.label }}</i>
      </p>
      <p class="cart-text">{{ dataset.variables_agg.count }} variables</p>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    dataset: Object,
    collectionAcronym: String,
    providerAcronym: String,
    networkAcronym: String,
  },
  computed: {
    datasetView() {
      if (this.$route.name.startsWith("provider")) {
        return "provider-dataset";
      }
      if (this.$route.name.startsWith("network")) {
        return "network-dataset";
      }
      if (this.$route.name.startsWith("collection")) {
        return "collection-dataset";
      }
      return "dataset";
    },
  },
};
</script>
