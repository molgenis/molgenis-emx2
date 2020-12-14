<template>
  <ul class="nav nav-tabs">
    <li class="nav-item">
      <RouterLink
        class="nav-link"
        :class="{ active: selected == 'variables' }"
        href="#"
        :to="{
          name: 'dataset-variables',
          params: { datasetName, collectionAcronym },
        }"
      >
        Variables
      </RouterLink>
    </li>
    <li class="nav-item">
      <RouterLink
        class="nav-link"
        :class="{ active: selected == 'description' }"
        href="#"
        :to="{
          name: 'dataset-description',
          params: { datasetName, collectionAcronym },
        }"
      >
        Description
      </RouterLink>
    </li>
    <li class="nav-item">
      <RouterLink
        class="nav-link"
        :class="{ active: selected == 'harmonisations' }"
        href="#"
        :to="{
          name: 'dataset-harmonisations',
          params: { datasetName, collectionAcronym },
        }"
        >Harmonisations
      </RouterLink>
    </li>
  </ul>
</template>
<script>
export default {
  props: {
    selected: String,
    collectionAcronym: String,
    datasetName: String,
  },
};
</script>
