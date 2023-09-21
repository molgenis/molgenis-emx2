<template>
  <ul v-if="organisations">
    <li v-for="i in organisations" :key="i.id">
      <RouterLink :to="{ name: 'Organisations-details', params: { id: i.id } }">
        {{ i.id }} - {{ i.name }}
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
