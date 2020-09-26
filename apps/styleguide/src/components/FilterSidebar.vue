<template>
  <div>
    <div :key="timestamp">
      <Draggable
        :list="filters"
        handle=".card-header "
        ghost-class="border-primary"
      >
        <div slot="header">
          <label>
            <ShowHide
              v-model="filters"
              checkAttribute="showFilter"
              @input="updateTimestamp"
              >Filters
            </ShowHide>
          </label>
        </div>
        <FilterContainer
          v-for="(column, idx) in filters"
          :title="column.name"
          :visible="column.showFilter"
          :key="column.name + column.updateTime"
          @remove="hideFilter(idx)"
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
      </pre
        >
      </ShowMore>
    </div>
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
import ShowHide from "./ShowHide";
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
    ShowMore,
    ShowHide
  },
  props: {
    filters: Array
  },
  data() {
    return {
      timestamp: 0
    };
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
      return url.searchParams;
    }
  },
  created() {
    this.filters.forEach(column => {
      //we use updateTime to be able to know when to refresh a view
      column.updateTime = column.name + new Date().getTime();
      column.showFilter = true;
    });
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
