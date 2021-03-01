<template>
  <div>
    <div class="p-2 mb-2" :class="headerCss">
      <h6>
        <RouterLink to="/" :class="headerCss"> home</RouterLink>
        /
      </h6>
    </div>
    <TableExplorer :table="tableName" @click="openDetailView" />
  </div>
</template>

<script>
import { TableExplorer } from "@mswertz/emx2-styleguide";

const css = {
  Institutions: "bg-info text-white",
  Contacts: "bg-info text-white",
  Affiliations: "bg-info text-white",
  Datasources: "bg-warning text-dark",
  Databanks: "bg-danger text-white",
  Networks: "bg-success text-white",
  Models: "bg-primary text-white",
};

export default {
  components: {
    TableExplorer,
  },
  props: {
    tableName: String,
  },
  computed: {
    headerCss() {
      return css[this.tableName];
    },
    detailRouteName() {
      //detailRoute is name of table minus trailing 's'
      return this.tableName.toLowerCase().slice(0, -1);
    },
  },
  methods: {
    openDetailView(row) {
      // in case of table
      if (this.tableName == "Tables") {
        this.$router.push({
          name: this.detailRouteName,
          params: {
            acronym: row.release.resource.acronym,
            version: row.release.version,
            tableName: row.name,
          },
        });
      } else if (row.version) {
        this.$router.push({
          name: this.detailRouteName,
          params: { acronym: row.resource.acronym, version: row.version },
        });
      } else if (row.acronym) {
        this.$router.push({
          name: this.detailRouteName,
          params: { acronym: row.acronym },
        });
      } else {
        this.$router.push({
          name: this.detailRouteName,
          params: { name: row.name },
        });
      }
    },
  },
};
</script>
