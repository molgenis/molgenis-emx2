<template>
  <div class="card">
    <div class="card-body">
      <h5 class="card-title">
        <RouterLink
          :to="{
            name: tableView,
            params: {
              collectionAcronym: table.collection.acronym,
              consortiumAcronym: consortiumAcronym,
              tableName: table.name,
              providerAcronym: providerAcronym,
            },
          }"
        >
          {{ table.name }}
        </RouterLink>
      </h5>
      <div v-if="!databankAcronym && !consortiumAcronym">
        <p
          class="cart-text"
          v-if="table.collection.mg_tableclass.includes('Databank')"
        >
          <label>Databank:</label>
          <RouterLink
            :to="{
              name: 'table-databank',
              params: { databankAcronym: table.collection.acronym },
            }"
          >
            {{ table.collection.acronym }}
          </RouterLink>
        </p>
        <p class="cart-text" v-else>
          <label>Consortium:</label>
          <RouterLink
            :to="{
              name: 'table-consortium',
              params: { consortiumAcronym: table.collection.acronym },
            }"
          >
            {{ table.collection.acronym }}
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
    providerAcronym: String,
    consortiumAcronym: String,
  },
  computed: {
    tableView() {
      if (this.$route.name.startsWith("provider")) {
        return "provider-table";
      }
      if (this.$route.name.startsWith("consort")) {
        return "consortium-table";
      }
      if (this.$route.name.startsWith("databank")) {
        return "databank-table";
      }
      return "table";
    },
  },
};
</script>
