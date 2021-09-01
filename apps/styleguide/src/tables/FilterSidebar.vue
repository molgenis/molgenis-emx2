<template>
  <div>
    <div>
      <Draggable
        :list="filters"
        handle=".filter-header "
        ghost-class="border-primary"
      >
        <FilterContainer
          v-for="(column, index) in filters"
          :expanded.sync="filters[index].expanded"
          :title="column.name"
          :visible="column.showFilter && column.columnType != 'HEADING'"
          :conditions="column.conditions"
          :key="column.name"
          :style="column.showFilter ? '' : 'display: none'"
        >
          <FilterInput
            :conditions.sync="filters[index].conditions"
            @update:conditions="$emit('update:filters', filters)"
            :columnType="column.columnType"
            :refTable="column.refTable"
            :graphqlURL="graphqlURL"
          />
        </FilterContainer>
      </Draggable>
    </div>
  </div>
</template>

<script>
import FilterContainer from "./FilterContainer";
import FilterInput from "./FilterInput";
import Draggable from "vuedraggable";

export default {
  components: {
    FilterInput,
    FilterContainer,
    Draggable,
  },
  props: {
    filters: Array,
    graphqlURL: String,
  },
  created() {
    //default expand
    for (var idx in this.filters) {
      if (this.filters.expanded == undefined) {
        this.expandFilter(idx);
      }
    }
  },
  methods: {
    expandFilter(index) {
      let update = this.filters;
      update[index].expanded = true;
      this.$emit("update:filters", update);
    },
    collapseFilter(index) {
      let update = this.filters;
      update[index].expanded = false;
      this.$emit("update:filters", update);
    },
    hideFilter(idx) {
      let update = this.filters;
      update[idx].showFilter = false;
      this.$emit("update:filters", update);
    },
    flatten(obj, keyName) {
      let result = {};
      Object.keys(obj).forEach((key) => {
        var newKey = `${keyName}-${key}`;
        if (typeof obj[key] === "object") {
          result = {
            ...result,
            ...this.flatten(obj[key], newKey),
          };
        } else {
          result[newKey] = obj[key];
        }
      });
      return result;
    },
  },
};
</script>

<docs>
examples
```
<template>
  <div>
    <div class="row">
      <div class="col-3">
        <FilterSidebar :filters.sync="table.filters" graphqlURL="/CohortNetwork/graphql"/>
      </div>
      <div class="col-9">
        <FilterWells :filters.sync="table.filters"/>
        <pre>{{ table }}</pre>
      </div>
    </div>
  </div>


</template>

<script>
  export default {
    data: function () {
      return {
        table: {
          filters: [{
            "name": "orderId",
            "pkey": true,
            "columnType": "STRING", showFilter: true
          },
            {
              "name": "variables",
              "columnType": "REF",
              "refTable": "Variables", showFilter: true
            },
            {
              "name": "quantity",
              "columnType": "INT", showFilter: true
            },
            {
              "name": "price",
              "columnType": "DECIMAL", showFilter: true
            },
            {
              "name": "complete",
              "columnType": "BOOL", showFilter: true
            },
            {
              "name": "status",
              "columnType": "STRING", showFilter: true
            },
            {
              "name": "birthday",
              "columnType": "DATE", showFilter: true
            },
            {
              "name": "tags",
              "refTable": "AreasOfInformation",
              "graphqlURL": "/CohortNetworks/graphql",
              "columnType": "ONTOLOGY_ARRAY",
              "showFilter": true
            }]
        }
      }
    }
  }
</script>
```

</docs>
