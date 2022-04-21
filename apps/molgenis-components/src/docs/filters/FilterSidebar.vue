<template>
  <demo-item id="filter-sidebar" label="Filter Sidebar">
    <div class="row">
      <div class="col-3">
        <FilterSidebar :filters="filters" @updateFilters="onUpdate"></FilterSidebar>
      </div>
      <div class="col-9">
        <FilterWells :filters="filters" @updateFilters="onUpdate" />
        <pre>{{ filters }}</pre>
      </div>
    </div>
  </demo-item>
</template>
<script>
export default {
  data: function () {
    return {
      filters: [
        {
          name: "orderId",
          pkey: true,
          columnType: "STRING",
          showFilter: true,
          conditions: ["test123"]
        },
        {
          name: "variables",
          columnType: "REF",
          refTable: "Variables",
          showFilter: true,
          conditions: []
        },
        {
          name: "quantity",
          columnType: "INT",
          showFilter: true,
          conditions: []
        },
        {
          name: "price",
          columnType: "DECIMAL",
          showFilter: true,
          conditions: []
        },
        {
          name: "complete",
          columnType: "BOOL",
          showFilter: true,
          conditions: []
        },
        {
          name: "status",
          columnType: "STRING",
          showFilter: true,
          conditions: []
        },
        {
          name: "birthday",
          columnType: "DATE",
          showFilter: true,
          conditions: []
        },
        {
          name: "tags",
          refTable: "Tag",
          columnType: "ONTOLOGY_ARRAY",
          showFilter: true,
          conditions: []
        },
      ],
    };
  },
  methods: {
    showAlert() {
      alert("clicked");
    },
    onUpdate(update) {
      this.filters = update
    }
  },
};
</script>