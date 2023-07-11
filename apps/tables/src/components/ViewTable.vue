<template>
  <div v-if="schema">
    <router-link v-if="schema" to="/"> &lt; {{ schema.name }} </router-link> /
    {{ table }}
    <RoutedTableExplorer
      :tableName="table"
      :schemaName="schema.name"
      :canView="canView"
      :canEdit="canEdit"
      :canManage="canManage"
      :locale="session?.locale"
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
    canView() {
      if (this.session) {
        const isAdmin = this.session.email === "admin";
        const roles = this.session.roles;
        const isViewer =
          (roles && roles.includes("Viewer")) ||
          this.activeTable.tableType === "ONTOLOGIES";
        const isEditor = roles && roles.includes("Editor");
        const isManager = roles && roles.includes("Manager");
        return isAdmin || isEditor || isManager || isViewer;
      } else {
        return false;
      }
    },
    canEdit() {
      if (this.session) {
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
