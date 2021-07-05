<template>
  <ul v-if="partners && partners.length > 0">
    <li v-for="p in partners" :key="p.acronym">
      <RouterLink
        :to="{
          name: 'institution',
          params: { acronym: p.institution.acronym },
        }"
      >
        {{ p.institution.acronym }} {{ p.institution.name }}
        {{ p.department }}
        <OntologyTerms :terms="[p.role]" :inline="true" :color="color" />
      </RouterLink>
    </li>
  </ul>
  <p v-else>N/A</p>
</template>

<script>
import OntologyTerms from "./OntologyTerms";

export default {
  components: {
    OntologyTerms,
  },
  props: {
    partners: Array,
    color: {
      type: String,
      default: () => "primary",
    },
  },
};
</script>
