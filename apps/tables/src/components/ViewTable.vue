<template>
  <div v-if="schema">
    <router-link v-if="schema" to="/"> &lt; {{ schema.id }} </router-link> /
    {{ table }}
    <MessageWarning v-if="session && !canView">
      You don't have permission to view this table. Please sign in or contact
      your administrator to request access.
    </MessageWarning>
    <RoutedTableExplorer
      v-else
      :tableId="table"
      :schemaId="schema.id"
      :canView="canView"
      :canEdit="canEdit"
      :canInsert="canInsert"
      :canUpdate="canUpdate"
      :canDelete="canDelete"
      :canManage="canManage"
      :locale="session?.locale"
      :activeRoles="session?.activeRoles"
    />
  </div>
</template>
<script>
import { RoutedTableExplorer, MessageWarning } from "molgenis-components";

export default {
  name: "ViewTable",
  props: {
    table: { type: String, required: true },
    schema: { type: Object, default: null },
    session: { session: Object },
  },
  components: {
    RoutedTableExplorer,
    MessageWarning,
  },
  computed: {
    canView() {
      const isViewer =
        this.session?.activeRoles?.some((r) => r.name === "Viewer") ||
        this.activeTable?.tableType === "ONTOLOGIES" ||
        this.hasTablePermission("select");
      return isViewer || this.canEdit;
    },
    canInsert() {
      const isEditor = this.session?.activeRoles?.some(
        (r) => r.name === "Editor"
      );
      return !!(isEditor || this.hasTablePermission("insert"));
    },
    canUpdate() {
      const isEditor = this.session?.activeRoles?.some(
        (r) => r.name === "Editor"
      );
      return !!(isEditor || this.hasTablePermission("update"));
    },
    canDelete() {
      const isEditor = this.session?.activeRoles?.some(
        (r) => r.name === "Editor"
      );
      return !!(isEditor || this.hasTablePermission("delete"));
    },
    canEdit() {
      return this.canInsert || this.canUpdate || this.canDelete;
    },
    canManage() {
      const isAdmin = this.session?.email === "admin";
      const isManager = this.session?.activeRoles?.some(
        (r) => r.name === "Manager"
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
