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
      :session="session"
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
        this.session?.activeRoles?.some((r) =>
          ["Viewer", "Editor", "Manager", "Owner"].includes(r.name)
        ) ||
        this.activeTable?.tableType === "ONTOLOGIES" ||
        this.hasTablePermission("select");
      return isViewer || this.canEdit;
    },
    canEdit() {
      const isEditor = this.session?.activeRoles?.some((r) =>
        ["Editor", "Manager", "Owner"].includes(r.name)
      );
      return (
        isEditor ||
        this.canManage ||
        this.hasTablePermission("insert") ||
        this.hasTablePermission("update")
      );
    },
    canManage() {
      const isAdmin = this.session?.email === "admin";
      const isManager = this.session?.activeRoles?.some((r) =>
        ["Manager", "Owner"].includes(r.name)
      );
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
  methods: {
    hasTablePermission(permission) {
      return this.session?.activeRoles?.some((role) =>
        role.permissions?.some(
          (p) =>
            (p.table === "*" || p.table === this.table) &&
            (permission === "select"
              ? p.select === true
              : p[permission] === true)
        )
      );
    },
  },
};
</script>
