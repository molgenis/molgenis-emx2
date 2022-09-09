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
      return (
        this.session &&
        (this.session.email == "admin" ||
          (this.session.roles &&
            (this.session.roles.includes("Editor") ||
              this.session.roles.includes("Manager"))))
      );
    },
    canManage() {
      return (
        this.session &&
        (this.session.email == "admin" ||
          this.session.roles.includes("Manager"))
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
