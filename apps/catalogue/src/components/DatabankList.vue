<template>
  <ul v-if="databanks">
    <li v-for="d in databanks" :key="d.acronym">
      <RouterLink :to="{ name: 'databank', params: { acronym: d.acronym } }"
        >{{ d.acronym }} - {{ d.name }}
      </RouterLink>
    </li>
  </ul>
  <p v-else>N/A</p>
</template>
<script>
export default {
  props: {
    databanks: Array,
  },
};
</script>
