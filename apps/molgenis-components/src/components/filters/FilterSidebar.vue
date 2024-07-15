<template>
  <div class="sidebar-container">
    <FilterContainer
      v-for="(filter, index) in visibleFilters"
      :key="filter.id"
      :title="filter.label"
      :conditions="filter.conditions"
    >
      <FilterInput
        :id="'filter-' + filter.id"
        :conditions="visibleFilters[index].conditions"
        @updateConditions="handleUpdateFilter(index, $event)"
        :columnType="filter.columnType"
        :tableId="filter.refTableId"
        :schemaId="filter.refSchemaId ? filter.refSchemaId : schemaId"
        :refLabel="filter.refLabel ? filter.refLabel : filter.refLabelDefault"
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
    schemaId: {
      type: String,
      required: false,
    },
  },
  computed: {
    visibleFilters() {
      return this.filters.filter(
        (column) => column.showFilter && column.columnType !== "HEADING"
      );
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
        <FilterSidebar :filters="filters" schemaId="pet store" @updateFilters="onUpdate"/>
      </div>
      <div class="col-8">
        <FilterWells :filters="filters" schemaId="pet store" @updateFilters="onUpdate"/>
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
            id: "orderId",
            label: "Order id",
            pkey: true,
            columnType: "STRING",
            showFilter: true,
            conditions: ["test123"],
          },
          {
            id: "pets",
            label: "Pets",
            columnType: "REF",
            showFilter: false,
            expanded: true,
            conditions: [],
            refTable: "Pet",
          },
          {
            id: "quantity",
            label: "Quantity",
            columnType: "INT",
            showFilter: true,
            conditions: []
          },
          {
            id: "longQuantity",
            label: "Long quantity",
            columnType: "LONG",
            showFilter: true,
            conditions: []
          },
          {
            id: "price",
            label: "Price",
            columnType: "DECIMAL",
            showFilter: true,
            conditions: []
          },
          {
            id: "complete",
            label: "Complete",
            columnType: "BOOL",
            showFilter: true,
            conditions: []
          },
          {
            id: "status",
            label: "Status",
            columnType: "STRING",
            showFilter: true,
            conditions: []
          },
          {
            id: "birthday",
            label: "Birthday",
            columnType: "DATE",
            showFilter: true,
            conditions: []
          },
          {
            id: "tags",
            label: "Tags",
            columnType: "ONTOLOGY_ARRAY",
            showFilter: true,
            conditions: [],
            refTableId: "Tag",
          },
          {
            id: "orders",
            label: "Orders",
            columnType: "REF_ARRAY",
            showFilter: true,
            conditions: [],
            refTableId: "Order",
            refLabel: "${orderId}"
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
