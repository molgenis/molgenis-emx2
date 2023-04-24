<template>
  <div>loading {{ id }}</div>
</template>

<script>
import { Client } from "molgenis-components";

/** will forward from Resource to specific details view, e.g. Databanks-details, based on mg_tableclass */
export default {
  props: {
    id: { type: String, required: true },
  },
  data() {
    return { resourceData: null };
  },
  computed: {
    resource() {
      if (this.resourceData && this.resourceData.mg_tableclass) {
        return this.resourceData.mg_tableclass.split(".")[1];
      } else {
        return null;
      }
    },
  },
  watch: {
    resourceData() {
      if (this.resourceData) {
        this.$router.replace({
          name: this.resource + "-details",
          params: { id: this.id },
        });
      }
    },
  },
  async mounted() {
    this.client = Client.newClient();
    this.resourceData = (
      await this.client.fetchTableDataValues("Resources", {
        filter: { id: { equals: this.id } },
      })
    )[0];
  },
};
</script>
