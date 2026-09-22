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
      :isRowLevel="isRowLevel"
      :userRoles="userRoles"
      :locale="session?.locale"
      :tablePermissions="session?.tablePermissions"
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
    tablePermission() {
      return this.session?.tablePermissions?.find((p) => p.id === this.table);
    },
    canView() {
      return (
        this.tablePermission?.canView ||
        this.activeTable?.tableType === "ONTOLOGIES" ||
        false
      );
    },
    canInsert() {
      return this.tablePermission?.canInsert || false;
    },
    canUpdate() {
      return this.tablePermission?.canUpdate || false;
    },
    canDelete() {
      return this.tablePermission?.canDelete || false;
    },
    canEdit() {
      return this.canInsert || this.canUpdate || this.canDelete || false;
    },
    canManage() {
      const isAdmin = this.session?.email === "admin";
      const isManager = this.session?.roles?.includes("Manager");
      return isManager || isAdmin;
    },
    isRowLevel() {
      return this.tablePermission?.isRowLevel || false;
    },
    userRoles() {
      return this.session?.roles || [];
    },
    activeTable() {
      if (this.schema) {
        return this.schema.tables.find((table) => table.id === this.table);
      } else {
        return null;
      }
    },
  },
  methods: {},
};
</script>
