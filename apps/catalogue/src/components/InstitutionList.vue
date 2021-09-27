<template>
  <ul v-if="institutions">
    <li v-for="i in institutions" :key="i.pid">
      <RouterLink
        :to="{ name: 'Institutions-details', params: { pid: i.pid } }"
      >
        {{ i.pid }} - {{ i.name }}
      </RouterLink>
    </li>
  </ul>
  <p v-else>N/A</p>
</template>

<script>
export default {
  props: {
    institutions: Array,
  },
};
</script>
