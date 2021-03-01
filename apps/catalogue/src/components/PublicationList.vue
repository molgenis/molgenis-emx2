<template>
  <ul>
    <li v-for="p in publications">
      <a :href="'https://doi.org/' + p.doi">
        {{ p.authors[0] }}{{ p.authors.length > 0 ? " et al" : "" }} ({{
          p.year
        }}) {{ p.journal }}
      </a>
    </li>
  </ul>
</template>
<script>
export default {
  props: {
    publications: Array,
  },
};
</script>
