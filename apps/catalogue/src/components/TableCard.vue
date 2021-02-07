<template>
  <div class="card">
    <div class="card-body">
      <h5 class="card-title">
        <RouterLink
          :to="{
            name: tableView,
            params: {
              resourceAcronym: table.resource.acronym,
              projectAcronym: projectAcronym,
              tableName: table.name,
              institutionAcronym: institutionAcronym,
            },
          }"
        >
          {{ table.name }}
        </RouterLink>
      </h5>
      <div v-if="!databankAcronym && !projectAcronym">
        <p
          class="cart-text"
          v-if="table.resource.mg_tableclass.includes('Databank')"
        >
          <label>Databank:</label>
          <RouterLink
            :to="{
              name: 'table-databank',
              params: { databankAcronym: table.resource.acronym },
            }"
          >
            {{ table.resource.acronym }}
          </RouterLink>
        </p>
        <p class="cart-text" v-else>
          <label>Project:</label>
          <RouterLink
            :to="{
              name: 'table-project',
              params: { projectAcronym: table.resource.acronym },
            }"
          >
            {{ table.resource.acronym }}
          </RouterLink>
        </p>
      </div>
      <p class="cart-text">
        <i>{{ table.label }}</i>
      </p>
      <p class="cart-text">{{ table.variables_agg.count }} variables</p>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    table: Object,
    databankAcronym: String,
    institutionAcronym: String,
    projectAcronym: String,
  },
  computed: {
    tableView() {
      if (this.$route.name.startsWith("instit")) {
        return "institution-table";
      }
      if (this.$route.name.startsWith("project")) {
        return "project-table";
      }
      if (this.$route.name.startsWith("databank")) {
        return "databank-table";
      }
      return "table";
    },
  },
};
</script>
