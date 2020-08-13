<template>
  <div :key="timestamp">
    <Draggable
      :list="filters"
      handle=".filter-drag-icon"
      ghost-class="border-primary"
    >
      <div slot="header">
        <label>Filters: </label>&nbsp;
        <a href="#" @click.prevent="collapseAll">collapse</a>&nbsp;
        <a href="#" @click.prevent="expandAll">expand</a>
      </div>
      <FilterContainer
        v-for="(column, idx) in filters"
        :title="column.name"
        :key="column.name + column.updateTime + column.collapsed"
        :collapsed="column.collapsed"
        @click="toggleCollapse(idx)"
        @collapse="collapse(idx)"
        @uncollapse="uncollapse(idx)"
      >
        <InputString
          :list="true"
          v-if="
            column.columnType.startsWith('STRING') ||
              column.columnType.startsWith('TEXT') ||
              column.columnType.startsWith('UUID')
          "
          v-model="column.conditions"
          :defaultValue="column.conditions"
        />
        <InputRangeInt
          :list="true"
          v-if="column.columnType.startsWith('INT')"
          v-model="column.conditions"
          :defaultValue="column.conditions"
        />
        <InputRangeDecimal
          :list="true"
          v-if="column.columnType.startsWith('DECIMAL')"
          v-model="column.conditions"
          :defaultValue="column.conditions"
        />
        <InputRangeDate
          :list="true"
          v-if="column.columnType.startsWith('DATE')"
          v-model="column.conditions"
          :defaultValue="column.conditions"
        />
        <InputCheckbox
          :list="true"
          v-if="column.columnType.startsWith('BOOL')"
          :options="['true', 'false']"
          v-model="column.conditions"
          :defaultValue="column.conditions"
        />
        <InputRef
          :list="true"
          v-if="column.columnType.startsWith('REF')"
          :refTable="column.refTable"
          v-model="column.conditions"
          :defaultValue="column.conditions"
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
</template>

<script>
import FilterContainer from "./FilterContainer";
import InputCheckbox from "./InputCheckbox";
import InputString from "./InputString";
import InputRangeInt from "./InputRangeInt";
import InputRangeDecimal from "./InputRangeDecimal";
import InputRangeDate from "./InputRangeDate";
import InputRef from "./InputRef";
import ShowMore from "./ShowMore";
import Draggable from "vuedraggable";

export default {
  components: {
    FilterContainer,
    Draggable,
    InputCheckbox,
    InputString,
    InputRangeInt,
    InputRangeDecimal,
    InputRangeDate,
    InputRef,
    ShowMore
  },
  props: {
    timestamp: Number,
    filters: Array
  },
  computed: {
    url() {
      let url = new URL("#");
      this.filters.forEach(column => {
        if (Array.isArray(column.conditions)) {
          column.conditions.forEach(value => {
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
                Object.keys(flatten).forEach(key => {
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
          this.filters.map(column => column.name).join("-")
        );
      }
      if (this.filters.length > 0) {
        url.searchParams.append(
          "_collapse",
          this.filters
            .filter(column => column.collapsed)
            .map(column => column.name)
            .join("-")
        );
      }
      return url.searchParams;
    }
  },
  created() {
    this.filters.forEach(column => {
      //we use updateTime to be able to know when to refresh a view
      column.updateTime = column.name + new Date().getTime();
    });
  },
  methods: {
    collapseAll() {
      this.filters.forEach(column => {
        column.collapsed = true;
      });
      this.timestamp = new Date().getTime();
    },
    expandAll() {
      this.filters.forEach(column => {
        column.collapsed = false;
      });
      this.timestamp = new Date().getTime();
    },
    collapse(idx) {
      this.filters[idx].collapsed = true;
      this.timestamp = new Date().getTime();
    },
    uncollapse(idx) {
      this.filters[idx].collapsed = false;
      this.timestamp = new Date().getTime();
    },
    toggleCollapse(idx) {
      if (this.filters[idx].collapsed) {
        this.uncollapse(idx);
      } else {
        this.collapse(idx);
      }
    },
    flatten(obj, keyName) {
      let result = {};
      Object.keys(obj).forEach(key => {
        var newKey = `${keyName}-${key}`;
        if (typeof obj[key] === "object") {
          result = {
            ...result,
            ...this.flatten(obj[key], newKey)
          };
        } else {
          result[newKey] = obj[key];
        }
      });
      return result;
    }
  }
};
</script>

<docs>
    examples
    ```
    <template>
        <div>
            <div class="row">
                <div class="col col-lg-5">
                    <FilterSidebar :filters="filters"/>
                </div>
                <div class="col">
                    <FilterWells :filters="filters"/>
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
                        "columnType": "STRING"
                    },
                        {
                            "name": "code",
                            "columnType": "REF",
                            "refTable": "Code",
                        },
                        {
                            "name": "quantity",
                            "columnType": "INT"
                        },
                        {
                            "name": "price",
                            "columnType": "DECIMAL"
                        },
                        {
                            "name": "complete",
                            "columnType": "BOOL"
                        },
                        {
                            "name": "status",
                            "columnType": "STRING"
                        },
                        {
                            "name": "birthday",
                            "columnType": "DATE"
                        }]
                }
            }
        }
    </script>
    ```

</docs>
