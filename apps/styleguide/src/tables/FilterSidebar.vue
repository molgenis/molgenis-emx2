<template>
  <div>
    <div :key="timestamp">
      <Draggable
        :list="filters"
        handle=".filter-header "
        ghost-class="border-primary"
      >
        <FilterContainer
          v-for="(column, index) in filters"
          :title="column.name"
          :visible="column.showFilter"
          :key="column.name + column.updateTime"
          :count="
            Array.isArray(column.conditions) ? column.conditions.length : null
          "
          :expanded="column.name == expanded"
          @expand="expanded = column.name"
          @collapse="expanded = null"
        >
          <FilterInput v-model="filters[index]" />
        </FilterContainer>
      </Draggable>
      <ShowMore title="debug">
        <pre>
url = {{ url }}
expanded = {{ expanded }}
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
    value: Array,
  },
  data() {
    return {
      filters: [],
      timestamp: 0,
      expanded: null,
    };
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
    this.filters = this.value;
    this.initShowFilter();
  },
  watch: {
    value() {
      this.filters = this.value;
    },
    filters() {
      //this.initShowFilter();
      this.$emit("input", this.filters);
    },
  },
  methods: {
    updateTimestamp() {
      this.timestamp = new Date().getTime();
    },
    hideFilter(idx) {
      this.filters[idx].showFilter = false;
      this.updateTimestamp();
      console.log("hide " + idx);
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
    initShowFilter() {
      this.filters.forEach((column) => {
        if (column.showFilter === undefined) {
          //we use updateTime to be able to know when to refresh a view
          column.updateTime = column.name + new Date().getTime();
          column.showFilter = true;
        }
      });
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
      <div class="col col-lg-5">
        <FilterSidebar v-model="filters"/>
      </div>
      <div class="col">
        <FilterWells v-model="filters"/>
      </div>
    </div>
  </div>


</template>

<script>
  export default {
    data: function () {
      return {
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
</script>
```

</docs>
