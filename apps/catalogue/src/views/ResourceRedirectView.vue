<template>
  <div>loading {{ resource }} {{ pid }}</div>
</template>

<script>
import { Client } from "molgenis-components";

/** will forward from Resource to specific details view, e.g. Databanks-details, based on mg_tableclass */
export default {
  computed: {
    resource() {
      if (this.resourceData && this.resourceData.mg_tableclass) {
        return this.resourceData.mg_tableclass.split(".")[1];
      } else {
        return null;
      }
    },
    pid() {
      if (this.resourceData && this.resourceData.pid) {
        return this.resourceData.pid;
      } else {
        return null;
      }
    },
  },
  watch: {
    resourceData() {
      if (this.resource) {
        this.$router.push({
          name: this.resource + "-details",
          params: { pid: this.pid },
        });
      }
    },
  },
  async mounted() {
    this.client = Client.newClient();
    this.resourceData = (
      await this.client.fetchTableDataValues(this.table, {
        filter: this.filter,
      })
    )[0];
  },
};
</script>
