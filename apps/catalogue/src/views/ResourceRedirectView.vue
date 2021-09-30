<template>
  <div>loading {{ resource }} {{ pid }}</div>
</template>

<script>
import { TableMixin } from "@mswertz/emx2-styleguide";

/** will forward from Resource to specific details view, e.g. Databanks-details, based on mg_tableclass */
export default {
  extends: TableMixin,
  computed: {
    row() {
      return this.data[0];
    },
    resource() {
      if (this.row && this.row.mg_tableclass) {
        return this.row.mg_tableclass.split(".")[1];
      }
    },
    pid() {
      if (this.row && this.row.pid) {
        return this.row.pid;
      }
    },
  },
  watch: {
    row() {
      if (this.resource) {
        this.$router.push({
          name: this.resource + "-details",
          params: { pid: this.pid },
        });
      }
    },
  },
};
</script>
