<template>
  <div class="from-inline" v-if="countFilters > 0">
    {{ countFilters }} filter{{ countFilters > 1 ? "s" : "" }} (<a
      href="#"
      @click.prevent="removeAll"
      >remove all filters</a
    >) :<br />
    <span v-for="(column, idx) in filters" :key="column.name">
      <span v-for="(value, idx2) in column.conditions" :key="idx2">
        <span v-if="Array.isArray(value)">
          <FilterWell
            v-if="value[0] !== null && value[1] !== null"
            @click="remove(idx, idx2)"
            :label="value[0] + ' &lt; ' + column.name + ' &lt; ' + value[1]"
          />
          <FilterWell
            v-else-if="value[0] !== null"
            @click="remove(idx, idx2)"
            :label="value[0] + ' &lt; ' + column.name"
          />

          <FilterWell
            v-else-if="value[1] !== null"
            @click="remove(idx, idx2)"
            :label="column.name + ' &lt; ' + value[1]"
          />
        </span>
        <span v-else>
          <FilterWell
            @click="remove(idx, idx2)"
            :label="column.name + ' = ' + renderValue(value)"
          />
        </span>
      </span>
    </span>
  </div>
</template>
<script>
import FilterWell from "./FilterWell";

export default {
  props: {
    /** two-way bindable array of column metadata. Will add 'conditions' property to hold filter values */
    filters: Array,
  },
  components: {
    FilterWell,
  },
  computed: {
    countFilters() {
      let count = 0;
      if (Array.isArray(this.filters)) {
        this.filters.forEach((column) => {
          if (Array.isArray(column.conditions)) {
            column.conditions.forEach((condition) => {
              if (Array.isArray(condition)) {
                if (condition[0] !== null || condition[1] != null) {
                  count++;
                }
              } else {
                if (condition != null) {
                  count++;
                }
              }
            });
          }
        });
      }
      return count;
    },
  },
  methods: {
    renderValue(value) {
      if (typeof value === "object" && value !== null) {
        return this.flattenObject(value);
      } else {
        return value;
      }
    },
    flattenObject(object) {
      let result = "";
      Object.keys(object).forEach((key) => {
        if (object[key] === null) {
          //nothing
        } else if (typeof object[key] === "object") {
          result += this.flattenObject(object[key]);
        } else {
          result += " " + object[key];
        }
      });
      return result;
    },
    remove(idx, idx2) {
      let update = this.filters;
      update[idx].conditions.splice(idx2, 1);
      this.$emit("update:filters", update);
    },
    removeAll() {
      let update = this.filters;
      for (var idx in update) {
        if (Array.isArray(update[idx].conditions)) {
          update[idx].conditions = [];
        }
      }
      this.$emit("update:filters", update);
    },
  },
};
</script>

<docs>
Example:
```
<template>
  <div>
    <FilterWells :filters.sync="filters"/>
    value: {{ JSON.stringify(filters) }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        filters:
            [
              {
                name: "orderId",
                conditions: []
              },
              {
                name: "pet",
                conditions: []
              },
              {
                name: "quantity",
                conditions: [[1, null]]
              },
              {
                name: "price",
                conditions: [[null, 25.7], [2, 11.3]]
              },
              {
                name: "complete",
                conditions: ["false"]
              },
              {
                name: "status",
                conditions: [{name: "status", second: " true"}]
              },
              {
                name: "birthday",
                conditions: [['2020-1-1', '2030-2-3']]
              }
            ]
      }
    }
  }
</script>
```


</docs>
