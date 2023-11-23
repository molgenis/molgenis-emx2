<template>
  <table class="table bg-white table-hover">
    <thead>
      <tr>
        <th scope="col">Label</th>
        <th scope="col">Description</th>
      </tr>
    </thead>
    <tr v-for="table in tables" :key="table.id">
      <td>
        <router-link :to="table.id">{{
          table.label === null || table.id
        }}</router-link>
      </td>
      <td>{{ table.description }}</td>
    </tr>
  </table>
</template>

<script>
export default {
  props: {
    tables: Array,
  },
};
</script>
