<template>
  <div>
    <div>
      <FilterContainer
        v-for="(filter, index) in visibleFilters"
        :key="filter.name"
        :title="filter.name"
        :conditions="filter.conditions"
      >
        <FilterInput
          :id="'filter-' + filter.name"
          :conditions="filters[index].conditions"
          @updateConditions="handleUpdateFilter(index, $event)"
          :columnType="filter.columnType"
        />
      </FilterContainer>
    </div>
  </div>
</template>

<script>
import FilterContainer from "./FilterContainer.vue";
import FilterInput from "./FilterInput.vue";

export default {
  name: "FilterSidebar",
  components: {
    FilterInput,
    FilterContainer,
  },
  props: {
    filters: Array,
  },
  computed: {
    visibleFilters() {
      return this.filters.filter(
        (column) => column.showFilter && column.columnType !== "HEADING"
      );
    },
  },
  methods: {
    handleUpdateFilter(index, event) {
      let newFilters = [...this.filters];
      newFilters[index].conditions = event;
      this.$emit("updateFilters", newFilters);
    },
  },
};
</script>

<docs>
<template>
  <demo-item>
    <div class="row">
      <div class="col-3">
        <FilterSidebar :filters="filters" @updateFilters="onUpdate"></FilterSidebar>
      </div>
      <div class="col-9">
        <FilterWells :filters="filters" @updateFilters="onUpdate"/>
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
</docs>
