<template>
  <div>
    <Draggable
      v-model="value.visible"
      handle=".card-header"
      ghost-class="border-primary"
    >
      <div slot="header" class="container m3">
        <label>collapse: </label>&nbsp;
        <a href="#" @click.prevent="collapseAll">all</a>&nbsp;
        <a href="#" @click.prevent="expandAll">none</a>
      </div>
      <FilterContainer
        class="m-3"
        v-for="column in value.visible"
        :title="column.name"
        :key="column.name"
        :collapse="value.collapsed.includes(column.name)"
        @collapse="value.collapsed.push(column.name)"
        @uncollapse="
          value.collapsed.splice(value.collapsed.indexOf(column.name), 1)
        "
      >
        <InputString
          :list="true"
          v-if="column.columnType === 'STRING'"
          v-model="value.conditions[column.name]"
          :defaultValue="value.conditions[column.name]"
        />
        <InputRangeInt
          :list="true"
          v-if="column.columnType === 'INT'"
          v-model="value.conditions[column.name]"
          :defaultValue="value.conditions[column.name]"
        />
        <InputRangeDecimal
          :list="true"
          v-if="column.columnType === 'DECIMAL'"
          v-model="value.conditions[column.name]"
          :defaultValue="value.conditions[column.name]"
        />
        <InputRangeDate
          :list="true"
          v-if="column.columnType === 'DATE'"
          v-model="value.conditions[column.name]"
          :defaultValue="value.conditions[column.name]"
        />
        <InputCheckbox
          :list="true"
          v-if="column.columnType === 'BOOL'"
          :options="['true', 'false']"
          v-model="value.conditions[column.name]"
          :defaultValue="value.conditions[column.name]"
        />
        <InputRef
          :list="true"
          v-if="column.columnType === 'REF'"
          :schema="schema"
          :refTable="column.refTable"
          :refColumn="column.refColumn"
          v-model="value.conditions[column.name]"
          :defaultValue="value.conditions[column.name]"
        />
      </FilterContainer>
    </Draggable>
    <br />
    url = {{ url }}
    <br />
    state = {{ value }}
  </div>
</template>

<style>
.card-header:hover {
  cursor: move;
}
</style>

<script>
import FilterContainer from "./FilterContainer";
import InputString from "./InputString";
import InputRangeInt from "./InputRangeInt";
import InputRangeDecimal from "./InputRangeDecimal";
import InputRangeDate from "./InputRangeDecimal";
import Draggable from "vuedraggable";

export default {
  components: {
    FilterContainer,
    Draggable,
    InputString,
    InputRangeInt,
    InputRangeDecimal,
    InputRangeDate
  },
  props: {
    columns: Array,
    schema: String,
    value: Object
  },
  // data() {
  //   return {
  //     value: {
  //       collapsed: [],
  //       visible: [],
  //       conditions: {}
  //     }
  //   }
  // },
  created() {
    this.columns.forEach(column => this.value.push(column));
  },
  computed: {
    url() {
      let url = new URL("#");
      Object.keys(this.value.conditions).forEach(name => {
        this.value.conditions[name].forEach(value => {
          if (Array.isArray(value)) {
            let rangeString = "";
            if (value[0] !== null) rangeString = value[0];
            rangeString += "..";
            if (value[1] !== null) rangeString += value[1];
            if (rangeString !== "..") {
              url.searchParams.append(name, encodeURIComponent(rangeString));
            }
          } else if (value !== null) {
            url.searchParams.append(name, encodeURIComponent(value));
          }
        });
      });
      if (this.value.visible.length > 0) {
        url.searchParams.append(
          "_show",
          this.value.visible.map(column => column.name).join("-")
        );
      }
      if (this.value.collapsed.length > 0) {
        url.searchParams.append("_collapse", this.value.collapsed.join("-"));
      }
      return url.searchParams;
    }
  },
  methods: {
    collapseAll() {
      this.value.visible.forEach(col => {
        if (!this.value.collapsed.includes(col.name))
          this.value.collapsed.push(col.name);
      });
    },
    expandAll() {
      this.value.collapsed = [];
    }
  },
  watch: {
    value: {
      deep: true, //needed to pick up deep changes
      handler() {
        this.$emit("input", this.value);
      }
    }
  }
};
</script>

<docs>

    examples
    ```
    <template>
        <div class="row">
            <div class="col col-lg-5">
                <FilterSidebar v-model="value" :columns="columns" :schema="schema"/>
            </div>
            <div class="col-auto" v-if="typeof value === 'object' && value !== null">
                <FilterWells v-model="value.conditions"/>
            </div>
            State: {{JSON.stringify(value)}}
        </div>

    </template>

    <script>
        export default {
            data: function () {
                return {
                    value: null,
                    schema: "pet%20store",
                    columns: [{
                        "name": "orderId",
                        "pkey": true,
                        "columnType": "STRING"
                    },
                        {
                            "name": "pet",
                            "columnType": "REF",
                            "refTable": "Pet",
                            "refColumn": "name"
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
