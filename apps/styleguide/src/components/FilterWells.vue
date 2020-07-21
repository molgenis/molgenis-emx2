<template>
  <div class="from-inline">
    <span v-if="countFilters > 0">
      {{ countFilters }} filters:&nbsp;
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
      <a href="#" @click.prevent="removeAll">
        remove all filters
      </a>
      <hr />
    </span>
  </div>
</template>
<script>
import FilterWell from "./FilterWell";

export default {
  props: {
    filters: Array
  },
  components: {
    FilterWell
  },
  computed: {
    countFilters() {
      let count = 0;
      if (Array.isArray(this.filters)) {
        this.filters.forEach(column => {
          if (Array.isArray(column.conditions)) {
            column.conditions.forEach(condition => {
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
    }
  },
  methods: {
    renderValue(value) {
      if (typeof value === "object" && value !== null) {
        return Object.values(value).join(" ");
      } else {
        return value;
      }
    },
    remove(idx, idx2) {
      this.filters[idx].conditions.splice(idx2, 1);
      //we use updateTime as key to know when to refresh a filter view
      this.filters[idx].updateTime = new Date().getTime();
    },
    removeAll() {
      this.filters.forEach(column => {
        column.conditions = [];
        //we use updateTime as key to know when to refresh a filter view
        column.updateTime = new Date().getTime();
      });
    }
  }
};
</script>

<docs>
    Example:
    ```
    <template>
        <div>
            <FilterWells :filters="filters"/>
            value: {{JSON.stringify(filters)}}
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
