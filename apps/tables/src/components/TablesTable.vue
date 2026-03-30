<template>
  <table class="table bg-white table-hover">
    <thead>
      <tr>
        <th scope="col">Label</th>
        <th scope="col">Description</th>
      </tr>
    </thead>
    <tr
      v-for="table in tables"
      :key="table.id"
      :class="{ 'text-muted': !hasPermission(table) }"
    >
      <td>
        <router-link v-if="hasPermission(table)" :to="table.id">{{
          table.label
        }}</router-link>
        <span v-else>{{ table.label }}</span>
      </td>
      <td>{{ table.description }}</td>
    </tr>
  </table>
</template>

<script>
export default {
  props: {
    tables: Array,
    tablePermissions: {
      type: Array,
      required: false,
      default: () => [],
    },
  },
  methods: {
    hasPermission(table) {
      if (!this.tablePermissions?.length) return true;
      const perm = this.tablePermissions.find((p) => p.name === table.name);
      return perm?.canView || table.tableType === "ONTOLOGIES";
    },
  },
};
</script>
