<template>
  <div v-if="schema">
    <router-link v-if="schema" to="/"> &lt; {{ schema.id }} </router-link> /
    {{ table }}
    <RoutedTableExplorer
      :tableId="table"
      :schemaId="schema.id"
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
      const isViewer =
        this.session?.roles?.includes("Viewer") ||
        this.activeTable.tableType === "ONTOLOGIES";
      return isViewer || this.canEdit;
    },
    canEdit() {
      const isEditor = this.session?.roles?.includes("Editor");
      return isEditor || this.canManage;
    },
    canManage() {
      const isAdmin = this.session?.email === "admin";
      const isManager = this.session?.roles?.includes("Manager");
      return isManager || isAdmin;
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
