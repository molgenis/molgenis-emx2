<template>
  <div>
    <router-link v-if="schema" to="/">
      &gt; Back to {{ schema.name }}
    </router-link>
    <TableExplorer
      :tableName="table"
      :key="timestamp"
      :canEdit="canEdit"
      :canManage="canManage"
    />
  </div>
</template>
<script>
import { TableExplorer } from "molgenis-components";

export default {
  name: "ViewTable",
  props: {
    table: { type: String, required: true },
    schema: { type: Object, default: null },
    session: { session: Object },
  },
  data() {
    return { timestamp: Date.now(), query: {} };
  },
  components: {
    TableExplorer,
  },
  computed: {
    canEdit() {
      return (
        this.session?.email == "admin" ||
        this.session?.roles?.includes("Editor") ||
        this.session?.roles?.includes("Manager")
      );
    },
    canManage() {
      return (
        this.session?.email == "admin" ||
        this.session?.roles?.includes("Manager")
      );
    },
    activeTable() {
      if (this.schema) {
        return this.schema.tables.filter((t) => t.name == this.table)[0];
      } else {
        return null;
      }
    },
  },
};
</script>
