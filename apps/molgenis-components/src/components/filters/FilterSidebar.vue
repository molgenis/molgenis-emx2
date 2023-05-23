<template>
  <div class="sidebar-container">
    <FilterContainer
      v-for="(filter, index) in visibleFilters"
      :key="filter.name"
      :title="filter.name"
      :conditions="filter.conditions"
    >
      <FilterInput
        :id="'filter-' + filter.name"
        :conditions="visibleFilters[index].conditions"
        @updateConditions="handleUpdateFilter(index, $event)"
        :columnType="filter.columnType"
        :tableName="filter.refTable"
        :schemaName="filter.refSchema ? filter.refSchema : schemaName"
      />
    </FilterContainer>
  </div>
</template>

<style scoped>
.sidebar-container {
  min-width: 16rem;
}
</style>

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
    filters: {
      type: Array,
      default: () => [],
    },
    schemaName: {
      type: String,
      required: false,
    },
  },
  computed: {
    visibleFilters() {
      return this.filters.filter((column) => column.showFilter && column.columnType !== "HEADING");
    },
  },
  methods: {
    handleUpdateFilter(index, newConditions) {
      let newFilters = [...this.visibleFilters];
      newFilters[index].conditions = newConditions;
      this.$emit("updateFilters", newFilters);
    },
  },
};
</script>

<docs>
<template>
  <demo-item>
    <div class="row">
      <div class="col-4">
        <FilterSidebar :filters="filters" schemaName="pet store" @updateFilters="onUpdate"/>
      </div>
      <div class="col-8">
        <FilterWells :filters="filters" schemaName="pet store" @updateFilters="onUpdate"/>
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
            name: "pets",
            columnType: "REF",
            showFilter: false,
            expanded: true,
            conditions: [],
            refTable: "Pet",
          },
          {
            name: "quantity",
            columnType: "INT",
            showFilter: true,
            conditions: []
          },
          {
            name: "longQuantity",
            columnType: "LONG",
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
            columnType: "ONTOLOGY_ARRAY",
            showFilter: true,
            conditions: [],
            refTable: "Tag",
          },
          {
            name: "orders",
            columnType: "REF_ARRAY",
            showFilter: true,
            conditions: [],
            refTable: "Order",
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
