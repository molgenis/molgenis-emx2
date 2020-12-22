<template>
  <ul class="nav nav-tabs">
    <li class="nav-item">
    <li class="nav-item">
      <RouterLink
          class="nav-link"
          :class="{ active: selected == 'description' }"
          href="#"
          :to="{
          name: 'collection-description',
          params: { collectionAcronym },
        }"
      >
        Description
      </RouterLink>
    </li>
    <RouterLink
        class="nav-link"
        :class="{ active: selected == 'datasets' }"
        href="#"
        :to="{
          name: 'collection-datasets',
          params: { collectionAcronym },
        }"
    >
      Datasets
    </RouterLink>
    </li>

    <li class="nav-item">
      <RouterLink
          class="nav-link"
          :class="{ active: selected == 'variables' }"
          href="#"
          :to="{
          name: 'collection-variables',
          params: { collectionAcronym },
        }"
      >
        Variables
      </RouterLink>
    </li>
  </ul>
</template>
<script>
export default {
  props: {
    selected: String,
    collectionAcronym: String,
  },
};
</script>
