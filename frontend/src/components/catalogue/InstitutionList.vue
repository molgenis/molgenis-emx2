<template>
  <ul v-if="institutions">
    <li v-for="i in institutions" :key="i.acronym">
      <RouterLink :to="{ name: 'institution', params: { acronym: i.acronym } }">
        {{ i.acronym }} - {{ i.name }}
      </RouterLink>
    </li>
  </ul>
  <p v-else>
    N/A
  </p>
</template>

<script>
export default {
  props: {
    institutions: Array,
  },
}
</script>
