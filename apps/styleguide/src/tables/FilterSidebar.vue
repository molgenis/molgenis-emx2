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
          :visible="column.showFilter && column.columnType != 'CONSTANT'"
          :count="count(column)"
          :key="column.name"
          :style="column.showFilter ? '' : 'display: none'"
        >
          <FilterInput
            :conditions.sync="filters[index].conditions"
            @update="$emit('update:filters', filters)"
            :columnType="column.columnType"
            :refTable="column.refTable"
          />
        </FilterContainer>
      </Draggable>
      <ShowMore title="debug">
        <pre>
url = {{ url }}
filters = {{ filters }}
      </pre
        >
      </ShowMore>
    </div>
  </div>
</template>

<script>
import FilterContainer from "./FilterContainer";
import FilterInput from "./FilterInput";
import ShowMore from "../layout/ShowMore";
import Draggable from "vuedraggable";

export default {
  components: {
    FilterInput,
    FilterContainer,
    Draggable,
    ShowMore,
  },
  props: {
    filters: Array,
  },
  created() {
    //default expand
    for (var idx in this.filters) {
      if (this.filters.expanded == undefined) {
        this.expandFilter(idx);
      }
    }
  },
  computed: {
    url() {
      let url = new URL("#");
      this.filters.forEach((column) => {
        if (Array.isArray(column.conditions)) {
          column.conditions.forEach((value) => {
            //range filters are nested arrays
            if (Array.isArray(value)) {
              let rangeString = "";
              if (value[0] !== null) rangeString = value[0];
              rangeString += "..";
              if (value[1] !== null) rangeString += value[1];
              if (rangeString !== "..") {
                url.searchParams.append(
                  column.name,
                  encodeURIComponent(rangeString)
                );
              }
            } else if (value !== null) {
              //refs are objects
              if (typeof value === "object") {
                let flatten = this.flatten(value, column.name);
                Object.keys(flatten).forEach((key) => {
                  url.searchParams.append(
                    key,
                    encodeURIComponent(flatten[key])
                  );
                });
                //otherwise treat as primitive
              } else {
                url.searchParams.append(column.name, encodeURIComponent(value));
              }
            }
          });
        }
      });
      if (this.filters.length > 0) {
        url.searchParams.append(
          "_show",
          this.filters.map((column) => column.name).join("-")
        );
      }
      return url.searchParams;
    },
  },
  methods: {
    count(column) {
      return Array.isArray(column.conditions) ? column.conditions.length : null;
    },
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
        <FilterSidebar :filters.sync="table.filters"/>
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
            }]
        }
      }
    }
  }
</script>
```

</docs>
