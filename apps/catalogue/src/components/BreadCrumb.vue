<template>
  <nav aria-label="breadcrumb">
    <ol class="breadcrumb">
      <li class="breadcrumb-item">
        <RouterLink to="/">Catalogue</RouterLink>
      </li>
      <!-- to show the main view we are in -->
      <li v-if="mainview" class="breadcrumb-item active">
        <RouterLink
          :to="{
            name: mainview,
          }"
        >
          {{ sanitize(mainview) }}
        </RouterLink>
      </li>
      <li
        v-if="institutionAcronym"
        class="breadcrumb-item active"
        aria-current="page"
      >
        <RouterLink
          :to="{
            name: 'institute',
            params: { institutionAcronym: this.institutionAcronym },
          }"
        >
          {{ institutionAcronym }}
        </RouterLink>
      </li>
      <li
        v-if="projectAcronym"
        class="breadcrumb-item active"
        aria-current="page"
      >
        <RouterLink
          :to="{
            name: projectview,
            params: { projectAcronym: this.projectAcronym },
          }"
        >
          {{ projectAcronym }}
        </RouterLink>
      </li>
      <li
        v-else-if="databankAcronym"
        class="breadcrumb-item active"
        aria-current="page"
      >
        <RouterLink
          :to="{
            name: databankview,
            params: { databankAcronym: this.databankAcronym },
          }"
        >
          {{ databankAcronym }}
        </RouterLink>
      </li>
      <li v-if="tableName" class="breadcrumb-item active" aria-current="page">
        <RouterLink
          :to="{
            name: tableView,
            params: {
              databankAcronym: this.databankAcronym,
              tableName: this.tableName,
            },
          }"
        >
          {{ tableName }}
        </RouterLink>
      </li>
    </ol>
  </nav>
</template>
<script>
export default {
  props: {
    databankAcronym: String,
    projectAcronym: String,
    institutionAcronym: String,
    tableName: String,
  },
  computed: {
    mainview() {
      if (this.$route.name) {
        if (this.$route.name.startsWith("institution")) {
          return "institutions";
        }
        if (this.$route.name.startsWith("consort")) {
          return "projects";
        }
        if (this.$route.name.startsWith("databank")) {
          return "databanks";
        }
        if (this.$route.name.startsWith("tables")) {
          return "tables";
        }
        if (this.$route.name.startsWith("variable")) {
          return "variables";
        }
      }
    },
    databankview() {
      if (this.$route.name.startsWith("institution")) {
        return "institution-databank";
      }
      if (this.$route.name.startsWith("table")) {
        return "table-databank";
      }
      return "databank";
    },
    projectview() {
      if (this.$route.name.startsWith("table")) {
        return "table-project";
      }
      return "project";
    },
    tableView() {
      if (this.$route.name.startsWith("databank")) {
        return "databank-table";
      }
      if (this.$route.name.startsWith("consort")) {
        return "project-table";
      }
      if (this.$route.name.startsWith("institution")) {
        return "institution-table";
      }
      return "table";
    },
  },
  methods: {
    sanitize(val) {
      return val.charAt(0).toUpperCase() + val.slice(1);
    },
  },
};
</script>
