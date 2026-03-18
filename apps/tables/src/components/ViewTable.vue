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
      :session="session"
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
    activeTable() {
      return this.schema?.tables?.find((table) => table.name === this.table);
    },
    canView() {
      return !!this.activeTable?.canView;
    },
    canInsert() {
      return !!this.activeTable?.canInsert;
    },
    canUpdate() {
      return !!this.activeTable?.canUpdate;
    },
    canDelete() {
      return !!this.activeTable?.canDelete;
    },
    canEdit() {
      return this.canInsert || this.canUpdate || this.canDelete;
    },
    canManage() {
      return !!this.schema?.canManage;
    },
  },
};
</script>
