<template>
  <div class="card">
    <div class="card-body">
      <h5 class="card-title">
        <RouterLink
          :to="{
            name: 'dataset-variables',
            params: {
              collectionAcronym: dataset.collection.acronym,
              datasetName: dataset.name,
            },
          }"
        >
          {{ dataset.name }}
        </RouterLink>
      </h5>
      <p class="cart-text">
        <i>{{ dataset.label }}</i>
      </p>
      <p class="cart-text">
        <RouterLink
          :to="{
            name: 'dataset-description',
            params: {
              collectionAcronym: dataset.collection.acronym,
              datasetName: dataset.name,
            },
          }"
        >
          Desciption
        </RouterLink>
      </p>
      <p class="cart-text">
        <RouterLink
          :to="{
            name: 'dataset-variables',
            params: {
              collectionAcronym: dataset.collection.acronym,
              datasetName: dataset.name,
            },
          }"
        >
          {{ dataset.variables_agg.count }} variables
        </RouterLink>
      </p>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    dataset: Object,
  },
};
</script>
