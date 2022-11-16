<template>
  <div class="sidebar-container">
    <Draggable
      v-model="filters"
      tag="span"
      item-key="name"
      handle=".filter-header "
      ghost-class="border-primary"
    >
      <template #item="{ element, index }">
        <FilterContainer :title="element.name" :conditions="element.conditions">
          <FilterInput
            v-if="element.columnType !== 'HEADING'"
            :id="'filter-' + element.name"
            :conditions="filters[index].conditions"
            @updateConditions="handleUpdateFilter(index, $event)"
            :columnType="element.columnType"
            :tableName="element.refTable"
            :graphqlURL="graphqlURL"
          />
        </FilterContainer>
      </template>
    </Draggable>
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
import Draggable from "vuedraggable";

export default {
  name: "FilterSidebar",
  components: {
    FilterInput,
    FilterContainer,
    Draggable,
  },
  props: {
    filters: {
      type: Array,
      default: () => [],
    },
    graphqlURL: {
      type: String,
      default: () => "graphql",
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
        <FilterSidebar :filters="filters" graphqlURL="/pet store/graphql" @updateFilters="onUpdate"/>
      </div>
      <div class="col-8">
        <FilterWells :filters="filters" graphqlURL="/pet store/graphql" @updateFilters="onUpdate"/>
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
