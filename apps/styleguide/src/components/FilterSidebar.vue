<template>
  <div :key="timestamp">
    <Draggable
      v-model="filters"
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
        v-for="(column, idx) in filters"
        :title="column.name"
        :key="column.name + column.updateTime + column.collapsed"
        :collapsed="column.collapsed"
        @collapse="collapse(idx)"
        @uncollapse="uncollapse(idx)"
      >
        <InputString
          :list="true"
          v-if="column.columnType === 'STRING'"
          v-model="column.conditions"
          :defaultValue="column.conditions"
        />
        <InputRangeInt
          :list="true"
          v-if="column.columnType === 'INT'"
          v-model="column.conditions"
          :defaultValue="column.conditions"
        />
        <InputRangeDecimal
          :list="true"
          v-if="column.columnType === 'DECIMAL'"
          v-model="column.conditions"
          :defaultValue="column.conditions"
        />
        <InputRangeDate
          :list="true"
          v-if="column.columnType === 'DATE'"
          v-model="column.conditions"
          :defaultValue="column.conditions"
        />
        <InputCheckbox
          :list="true"
          v-if="column.columnType === 'BOOL'"
          :options="['true', 'false']"
          v-model="column.conditions"
          :defaultValue="column.conditions"
        />
        <InputRef
          :list="true"
          v-if="column.columnType === 'REF'"
          :schema="schema"
          :refTable="column.refTable"
          :refColumn="column.refColumn"
          v-model="column.conditions"
          :defaultValue="column.conditions"
        />
      </FilterContainer>
    </Draggable>
    <br />
    <br />
    url = {{ url }}
  </div>
</template>

<style>
.card-header:hover {
  cursor: move;
}
</style>

<script>
import FilterContainer from './FilterContainer'
import InputString from './InputString'
import InputRangeInt from './InputRangeInt'
import InputRangeDecimal from './InputRangeDecimal'
import InputRangeDate from './InputRangeDate'
import Draggable from 'vuedraggable'

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
    schema: String,
    timestamp: Number,
    filters: Array
  },
  computed: {
    url() {
      let url = new URL('#')
      this.filters.forEach(column => {
        if (Array.isArray(column.conditions)) {
          column.conditions.forEach(value => {
            if (Array.isArray(value)) {
              let rangeString = ''
              if (value[0] !== null) rangeString = value[0]
              rangeString += '..'
              if (value[1] !== null) rangeString += value[1]
              if (rangeString !== '..') {
                url.searchParams.append(
                  column.name,
                  encodeURIComponent(rangeString)
                )
              }
            } else if (value !== null) {
              url.searchParams.append(column.name, encodeURIComponent(value))
            }
          })
        }
      })
      if (this.filters.length > 0) {
        url.searchParams.append(
          '_show',
          this.filters.map(column => column.name).join('-')
        )
      }
      if (this.filters.length > 0) {
        url.searchParams.append(
          '_collapse',
          this.filters
            .filter(column => column.collapsed)
            .map(column => column.name)
            .join('-')
        )
      }
      return url.searchParams
    }
  },
  created() {
    this.filters.forEach(column => {
      //we use updateTime to be able to know when to refresh a view
      column.updateTime = column.name + new Date().getTime()
    })
  },
  methods: {
    collapseAll() {
      this.filters.forEach(column => {
        column.collapsed = true
      })
      this.timestamp = new Date().getTime()
    },
    expandAll() {
      this.filters.forEach(column => {
        column.collapsed = false
      })
      this.timestamp = new Date().getTime()
    },
    collapse(idx) {
      this.filters[idx].collapsed = true
      this.timestamp = new Date().getTime()
    },
    uncollapse(idx) {
      this.filters[idx].collapsed = false
      this.timestamp = new Date().getTime()
    }
  }
}
</script>

<docs>
    examples
    ```
    <template>
        <div>
            <div class="row">
                <div class="col col-lg-5">
                    <FilterSidebar :filters="filters" :schema="schema"/>
                </div>
                <div class="col">
                    <FilterWells :filters="filters"/>
                </div>
            </div>
            State:
            <pre>{{JSON.stringify(filters,null, 2)}}</pre>
        </div>


    </template>

    <script>
        export default {
            data: function () {
                return {
                    schema: "pet%20store",
                    filters: [{
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
