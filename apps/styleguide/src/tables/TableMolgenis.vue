<template>
  <div class="table-responsive">
    <table
      class="table table-bordered table-sm"
      :class="{ 'table-hover': select }"
      :key="timestamp"
    >
      <thead>
        <Draggable
          :list="metadata.columns"
          handle=".column-drag-header"
          ghost-class="border-primary"
          tag="tr"
        >
          <th slot="header" scope="col" style="width: 1px;">
            <slot name="colheader" />
          </th>
          <th
            v-for="col in metadata.columns"
            :key="col.name + col.showColumn"
            scope="col"
            class="column-drag-header"
            :style="col.showColumn ? '' : 'display: none'"
          >
            <b>{{ col.name }}</b>
          </th>
        </Draggable>
      </thead>
      <tbody>
        <tr v-for="(row, idx) in data" :key="JSON.stringify(row)">
          <td>
            <div style="display: flex;">
              <div v-if="select" class="form-check form-check-inline mr-1">
                <input
                  type="checkbox"
                  class="form-check-input position-static"
                  :checked="isSelected(row)"
                  @click="onRowClick(row)"
                />
              </div>
              <slot
                name="rowheader"
                :row="row"
                :metadata="metadata"
                :rowkey="getKey(row)"
              />
            </div>
          </td>
          <td
            v-for="col in metadata.columns"
            :key="idx + col.name"
            @click="onRowClick(row)"
            style="cursor: pointer"
            :style="col.showColumn ? '' : 'display: none'"
          >
            <div v-if="'FILE' === col.columnType">
              <a
                :href="
                  'graphql?table=' +
                    metadata.name +
                    '&column=' +
                    col.name +
                    '&download=' +
                    row[col.name].id
                "
                >download.{{ row[col.name].extension }}</a
              >
              ({{ renderNumber(row[col.name].size) }}b)
            </div>
            <div v-else>
              <div
                v-for="(value, idx2) in renderValue(row, col)"
                :key="idx + col + idx2"
              >
                <div v-if="'TEXT' === col.columnType">
                  <ReadMore :text="value" />
                </div>
                <span v-else> {{ value }}</span>
              </div>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <ShowMore title="debug">
      <pre>
metadata = {{ JSON.stringify(metadata) }}

timestamp = {{ timestamp }}
     </pre
      >
    </ShowMore>
  </div>
</template>

<style scoped>
table {
  position: relative;
}

th {
  /*sticky header*/
  position: sticky;
  top: 0;
}

th,
td {
  text-align: left;
}

.column-drag-header:hover {
  cursor: grab;
}

.column-remove {
  float: right;
  top: 0px;
  right: 0px;
  margin: 0px;
  padding: 0px;
  visibility: hidden;
}

.column-drag-header:hover .column-remove {
  visibility: visible;
}
</style>

<script>
import Draggable from "vuedraggable";
import ShowMore from "../layout/ShowMore";
import ReadMore from "../layout/ReadMore";

export default {
  components: { Draggable, ShowMore, ReadMore },
  props: {
    select: {
      type: Boolean,
      default: false
    },
    metadata: Object,
    data: Array
  },
  data: () => {
    return {
      selectedItems: [],
      timestamp: 0
    };
  },
  methods: {
    updateTimestamp() {
      this.timestamp = new Date().getTime();
    },
    renderValue(row, col) {
      if (row[col.name] === undefined) {
        return [];
      }
      if (
        col.columnType == "REF_ARRAY" ||
        col.columnType == "MREF" ||
        col.columnType == "REFBACK"
      ) {
        return row[col.name].map(v => this.flattenObject(v));
      } else if (col.columnType == "REF") {
        return [this.flattenObject(row[col.name])];
      } else {
        return [row[col.name]];
      }
    },
    flattenObject(object) {
      let result = "";
      Object.keys(object).forEach(key => {
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
    getKey(row) {
      let result = {};
      this.metadata.columns
        .filter(c => c.key == 1)
        .map(c => (result[c.name] = row[c.name]));
      return result;
    },
    isSelected(row) {
      let key = this.getKey(row);
      return (
        this.selectedItems.filter(
          s => JSON.stringify(key) === JSON.stringify(s)
        ).length > 0
      );
    },
    onRowClick(row) {
      if (this.select) {
        let key = this.getKey(row);
        if (this.isSelected(row)) {
          /** when a row is deselected */
          this.selectedItems = this.selectedItems.filter(
            item => JSON.stringify(item) !== JSON.stringify(key)
          );
          this.$emit("deselect", this.getKey(row));
        } else {
          /** when a row is selected */
          this.selectedItems.push(this.getKey(row));
          this.$emit("select", this.getKey(row));
        }
      } else {
        this.$emit("click", this.getKey(row));
      }
    },
    renderNumber(number) {
      var SI_SYMBOL = ["", "k", "M", "G", "T", "P", "E"];

      // what tier? (determines SI symbol)
      var tier = (Math.log10(number) / 3) | 0;

      // if zero, we don't need a suffix
      if (tier == 0) return number;

      // get suffix and determine scale
      var suffix = SI_SYMBOL[tier];
      var scale = Math.pow(10, tier * 3);

      // scale the number
      var scaled = number / scale;

      // format number and add suffix
      return scaled.toFixed(1) + suffix;
    }
  },
  watch: {
    selectedItems() {
      this.$emit("input", this.selectedItems);
    },
    metadata: {
      deep: true,
      handler() {
        this.updateTimestamp();
      }
    }
  },
  created() {
    this.metadata.columns.forEach(c => {
      if (c["showColumn"] === undefined) c.showColumn = true;
    });
  }
};
</script>

<docs>
example
```
<template>
  <div>
    <MolgenisTable v-model="selected" :metadata="metadata" :data="data" @select="click" @deselect="click"
                   @click="click">
      <template v-slot:header>columns</template>
      <template v-slot:rowheader="slotProps">
        <RowButtonEdit
            table="Pet"
            :pkey="{name:'spike'}"
            @close="click"
        />
        <RowButtonDelete
            table="Pet"
            :pkey="{name:'spike'}"
            @close="click"
        />
      </template>
    </MolgenisTable>
    selected: {{ JSON.stringify(selected) }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        selected: [],
        metadata: {
          columns: [{name: 'col1', columnType: "STRING", key: 1}, {
            name: 'ref1',
            columnType: "REF",
            refColumns: ['firstName', 'lastName']
          }, {name: 'ref_arr1', columnType: "REF_ARRAY", refColumns: ['firstName', 'lastName']}]
        },
        data: [{
          col1: "row1",
          ref1: {firstName: 'katrien', lastName: 'duck'},
          ref_arr1: [{firstName: 'kwik', lastName: 'duck'}, {
            firstName: 'kwek',
            lastName: 'duck'
          }, {firstName: 'kwak', lastName: 'duck'}]
        }, {
          col1: "row2"
        }]
      }
    },
    methods: {
      click(value) {
        alert("click " + JSON.stringify(value));
      }
    }
  };
</script>
```
</docs>
