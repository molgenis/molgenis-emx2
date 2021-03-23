<template>
  <ul v-if="contributors">
    <li v-for="c in contributors">
      <RouterLink :to="{ name: 'contact', params: { name: c.contact.name } }">
        {{ c.contact.name }}
      </RouterLink>
      <OntologyTerms
        :color="color"
        :inline="true"
        :terms="c.contributionType"
      />
    </li>
  </ul>
  <p v-else>
    N/A
  </p>
</template>

<script>
import OntologyTerms from './OntologyTerms.vue'

export default {
  components: {OntologyTerms},
  props: {
    color: {
      type: String,
      default: () => 'primary',
    },
    contributors: Array,
  },
}
</script>
