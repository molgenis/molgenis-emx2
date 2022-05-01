<template>
  <span>
    <div v-if="countFilters > 0">
      <div class="d-flex align-items-center">
        {{ countFilters }} filter{{ countFilters > 1 ? "s" : "" }} (
        <button type="button" @click="removeAll" class="btn btn-link pl-0 pr-0">
          remove all filters
        </button>
        ) :
      </div>
      <span v-for="(facet, facetIndex) in filters" :key="facet.name">
        <span
          v-for="(condition, conditionIndex) in facet.conditions"
          :key="conditionIndex"
        >
          <span v-if="Array.isArray(condition)">
            <FilterWell
              v-if="condition[0] !== null && condition[1] !== null"
              @click="remove(facetIndex, conditionIndex)"
              :label="
                condition[0] + ' &lt; ' + facet.name + ' &lt; ' + condition[1]
              "
            />
            <FilterWell
              v-else-if="condition[0] !== null"
              @click="remove(facetIndex, conditionIndex)"
              :label="condition[0] + ' &lt; ' + facet.name"
            />

            <FilterWell
              v-else-if="condition[1] !== null"
              @click="remove(facetIndex, conditionIndex)"
              :label="facet.name + ' &lt; ' + condition[1]"
            />
          </span>
          <span v-else>
            <FilterWell
              @click="remove(facetIndex, conditionIndex)"
              :label="facet.name + ' = ' + renderValue(condition)"
            />
          </span>
        </span>
      </span>
    </div>
  </span>
</template>
<script>
import FilterWell from "./FilterWell.vue";

export default {
  props: {
    /** two-way bindable array of column metadata. Will add 'conditions' property to hold filter values */
    filters: { type: Array, default: () => [] },
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
    remove(facetIndex, conditionIndex) {
      let update = this.filters;
      update[facetIndex].conditions.splice(conditionIndex, 1);
      this.$emit("updateFilters", update);
    },
    removeAll() {
      let update = this.filters;
      for (var idx in update) {
        if (Array.isArray(update[idx].conditions)) {
          update[idx].conditions.splice(0); // use splice to avoid removing vue reactivity
        }
      }
      this.$emit("updateFilters", update);
    },
  },
};
</script>

<docs>
<template>
  <demo-item>
    <FilterWells :filters="filters" @updateFilters="onUpdate"/>
    value: {{ JSON.stringify(filters) }}
  </demo-item>
</template>
<script>
  export default {
    data: function () {
      return {
        filters: [
          {
            name: "orderId",
            conditions: [123],
          },
          {
            name: "pet",
            conditions: [],
          },
          {
            name: "quantity",
            conditions: [[1, null]],
          },
          {
            name: "price",
            conditions: [
              [null, 25.7],
              [2, 11.3],
            ],
          },
          {
            name: "complete",
            conditions: ["false"],
          },
          {
            name: "status",
            conditions: [{name: "status", second: " true"}],
          },
          {
            name: "birthday",
            conditions: [["2020-1-1", "2030-2-3"]],
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
