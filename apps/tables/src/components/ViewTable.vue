<template>
  <div>
    <router-link v-if="schema" to="/">
      &gt; Back to {{ schema.name }}
    </router-link>
    <RoutedTableExplorer
      :tableName="table"
      :canEdit="canEdit"
      :canManage="canManage"
    />
  </div>
</template>
<script>
import { RoutedTableExplorer } from "molgenis-components";

export default {
  name: "ViewTable",
  props: {
    table: { type: String, required: true },
    schema: { type: Object, default: null },
    session: { session: Object },
  },
  components: {
    RoutedTableExplorer,
  },
  computed: {
    canEdit() {
      if (this.session) {
        // Can't use var?.prop in this app :(
        const isAdmin = this.session.email === "admin";
        const roles = this.session.roles;
        const isEditor = roles && roles.includes("Editor");
        const isManager = roles && roles.includes("Manager");
        return isAdmin || isEditor || isManager;
      } else {
        return false;
      }
    },
    canManage() {
      return (
        this.session &&
        (this.session.email === "admin" ||
          this.session.roles.includes("Manager"))
      );
    },
    activeTable() {
      if (this.schema) {
        return this.schema.tables.find((table) => table.name === this.table);
      } else {
        return null;
      }
    },
  },
};
</script>
