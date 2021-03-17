<template>
    <div>
        <div>
            <Draggable
                ghost-class="border-primary"
                handle=".filter-header "
                :list="filters"
            >
                <FilterContainer
                    v-for="(column, index) in filters"
                    :key="column.name"
                    v-model:expanded="filters[index].expanded"
                    :count="count(column)"
                    :style="column.showFilter ? '' : 'display: none'"
                    :title="column.name"
                    :visible="column.showFilter"
                >
                    <FilterInput
                        v-model:conditions="filters[index].conditions"
                        :column-type="column.columnType"
                        :ref-table="column.refTable"
                        @update="$emit('update:filters', filters)"
                    />
                </FilterContainer>
            </Draggable>
            <ShowMore title="debug">
                <pre>
url = {{ url }}
filters = {{ filters }}
      </pre>
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
  created() {
    //default expand
    for (var idx in this.filters) {
      if (this.filters.expanded == undefined) {
        this.expandFilter(idx);
      }
    }
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
