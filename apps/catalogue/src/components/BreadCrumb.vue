<template>
  <nav aria-label="breadcrumb">
    <ol class="breadcrumb">
      <li class="breadcrumb-item">
        <RouterLink to="/collections">Collections</RouterLink>
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
          Collection: {{ collectionAcronym }}
        </RouterLink>
      </li>
      <li v-if="datasetName" class="breadcrumb-item active" aria-current="page">
        <RouterLink
          :to="{
            name: 'dataset',
            params: {
              collectionAcronym: this.collectionAcronym,
              datasetName: this.datasetName,
            },
          }"
        >
          Dataset: {{ datasetName }}
        </RouterLink>
      </li>
      <li
        v-if="view == 'variables'"
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
    datasetName: String,
  },
  computed: {
    view() {
      return this.$route.name;
    },
  },
};
</script>
