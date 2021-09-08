<template>
  <div style="max-width: 100%; overflow-x: auto" class="flex-grow-1">
    <table
      class="table table-sm bg-white table-bordered table-hover"
      :class="{ 'table-hover': showSelect }"
    >
      <thead>
        <Draggable
          :list="columns"
          handle=".column-drag-header"
          ghost-class="border-primary"
          tag="tr"
          @change="$emit('update:columns', $event)"
        >
          <th slot="header" scope="col" style="width: 1px" v-if="hasColheader">
            <h6 class="mb-0 mt-2 d-inline">
              #
              <!--@slot Use this to add values or actions buttons header -->
            </h6>
            <span style="text-align: left; font-weight: normal"
              ><slot name="colheader" />
            </span>
          </th>
          <th
            v-for="col in columnsWithoutMeta"
            :key="col.name + col.showColumn"
            scope="col"
            class="column-drag-header"
            :style="col.showColumn ? '' : 'display: none'"
          >
            <h6 class="mb-0">{{ col.name }}</h6>
          </th>
        </Draggable>
      </thead>
      <tbody>
        <tr
          v-for="(row, idx) in data"
          :key="idx + JSON.stringify(row) + isSelected(row)"
          :class="
            (isSelected(row) ? 'table-primary' : 'table-hover') +
            (row['mg_draft'] ? 'alert alert-warning' : '')
          "
        >
          <td v-if="hasColheader">
            <div style="display: flex">
              <div v-if="showSelect" class="form-check form-check-inline mr-1">
                <input
                  type="checkbox"
                  class="form-check-input position-static"
                  :checked="isSelected(row)"
                  @click="onRowClick(row)"
                />
              </div>
              <!--@slot Use this to add values or actions buttons to each row -->
              <slot
                name="rowheader"
                :row="row"
                :metadata="tableMetadata"
                :rowkey="getKey(row)"
              />
            </div>
            <i v-if="row.mg_draft" class="fas fa-user-edit">draft</i>
          </td>
          <td
            v-for="col in columnsWithoutMeta"
            :key="idx + col.name + isSelected(row)"
            style="cursor: pointer"
            :style="col.showColumn ? '' : 'display: none'"
            @click="onRowClick(row)"
          >
            <div v-if="'FILE' === col.columnType">
              <a v-if="row[col.name].id" :href="row[col.name].url">
                {{ col.name }}.{{ row[col.name].extension }} ({{
                  renderNumber(row[col.name].size)
                }}b)
              </a>
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
  </div>
</template>

<style scoped>
th {
  background-color: white;
}

.column-drag-header:hover {
  cursor: grab;
}

.column-drag-header:hover .column-remove {
  visibility: visible;
}
</style>

<script>
/**
 * Table that uses MOLGENIS metadata format to configure itself. Provide 'metadata' and 'data' and your table is ready.
 * Can be used without backend to configure a table. Note, columns can be dragged.
 */
import Draggable from "vuedraggable";
import ReadMore from "../layout/ReadMore";

export default {
  components: { Draggable, ReadMore },
  props: {
    /** selection, two-way binded*/
    selection: Array,
    /** column metadata, two-way binded to allow for reorder */
    columns: Array,
    /** not two way binded, table metadata */
    tableMetadata: Object,
    /** json structure in molgenis json data format matching the column metadata */
    data: Array,
    /** if select show be shown */
    showSelect: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    countColumns() {
      return this.columnsWithoutMeta.filter((c) => c.showColumn).length;
    },
    columnsWithoutMeta() {
      return this.columns.filter((c) => c.columnType != "HEADING");
    },
  },
  created() {
    this.initShowColumn();
  },
  methods: {
    hasColheader() {
      return (
        this.showSelect || !!this.$slots.colheader || !!this.$slots.rowheader
      );
    },
    initShowColumn() {
      if (this.columns) {
        let update = this.columns;
        for (var key in update) {
          if (update[key].showColumn == undefined) {
            update[key].showColumn = true;
            this.$emit("update:columns", update);
          }
        }
      }
    },
    renderValue(row, col) {
      if (row[col.name] === undefined) {
        return [];
      }
      if (
        ["REF_ARRAY", "REFBACK", "ONTOLOGY_ARRAY"].indexOf(col.columnType) > 0
      ) {
        return row[col.name].map((v) => {
          if (col.refLabel) {
            return this.applyJsTemplate(col.refLabel, v);
          } else {
            return this.flattenObject(v);
          }
        });
      } else if (col.columnType == "REF" || col.columnType == "ONTOLOGY") {
        if (col.refLabel) {
          return [this.applyJsTemplate(col.refLabel, row[col.name])];
        } else {
          return [this.flattenObject(row[col.name])];
        }
      } else if (col.columnType.includes("ARRAY") > 0) {
        return row[col.name];
      } else {
        return [row[col.name]];
      }
    },
    applyJsTemplate(template, object) {
      const names = Object.keys(object);
      const vals = Object.values(object);
      try {
        return new Function(...names, "return `" + template + "`;")(...vals);
      } catch (err) {
        return (
          err.message +
          " we got keys:" +
          JSON.stringify(names) +
          " vals:" +
          JSON.stringify(vals) +
          " and template: " +
          template
        );
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
          result += "." + object[key];
        }
      });
      return result.replace(/^\./, "");
    },
    getKey(row) {
      let result = {};
      this.columns
        .filter((c) => c.key == 1)
        .map((c) => (result[c.name] = row[c.name]));
      return result;
    },
    isSelected(row) {
      let key = this.getKey(row);
      let found = false;
      if (Array.isArray(this.selection)) {
        this.selection.forEach((s) => {
          if (s != null && this.deepEqual(key, s)) {
            found = true;
          }
        });
      }
      return found;
    },
    /** horrible that this is not standard, found this here https://dmitripavlutin.com/how-to-compare-objects-in-javascript/#4-deep-equality*/
    deepEqual(object1, object2) {
      const keys1 = Object.keys(object1);
      const keys2 = Object.keys(object2);
      if (keys1.length !== keys2.length) {
        return false;
      }
      for (const key of keys1) {
        const val1 = object1[key];
        const val2 = object2[key];
        const areObjects = this.isObject(val1) && this.isObject(val2);
        if (
          (areObjects && !this.deepEqual(val1, val2)) ||
          (!areObjects && val1 !== val2)
        ) {
          return false;
        }
      }
      return true;
    },
    isObject(object) {
      return object != null && typeof object === "object";
    },
    onRowClick(row) {
      //deep copy
      let update = JSON.parse(JSON.stringify(this.selection));
      if (this.showSelect) {
        let key = this.getKey(row);
        if (this.isSelected(row)) {
          /** when a row is deselected */
          update = update.filter(
            (item) =>
              JSON.stringify(item, Object.keys(item).sort()) !==
              JSON.stringify(key, Object.keys(key).sort())
          );
          this.$emit("deselect", this.getKey(row));
        } else {
          /** when a row is selected */
          update.push(this.getKey(row));
          this.$emit("select", this.getKey(row));
        }
      } else {
        this.$emit("click", this.getKey(row));
      }
      this.$emit("update:selection", update);
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
    },
  },
};
</script>

<docs>
example
```
<template>
  <div>
    <TableMolgenis :selection.sync="selected" :columns.sync="columns" :data="data" @select="click"
                   @deselect="click"
                   @click="click">
      <template v-slot:header>columns</template>
      <template v-slot:rowheader="slotProps">
        some row content
      </template>
    </TableMolgenis>
    selected: {{ JSON.stringify(selected) }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        selected: [],
        columns: [{name: 'col1', columnType: "STRING", key: 1}, {
          name: 'ref1',
          columnType: "REF",
          refColumns: ['firstName', 'lastName']
        }, {name: 'ref_arr1', columnType: "REF_ARRAY", refColumns: ['firstName', 'lastName']}]
        ,
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
example with default selection
```
<template>
  <div>
    <TableMolgenis :selection.sync="selected" :columns.sync="columns" :data="data" @select="click"
                   @deselect="click" @click="click"
                   :showSelect="true">
      <template v-slot:header>columns</template>
    </TableMolgenis>
    selected: {{ JSON.stringify(selected) }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        selected: [{'col1': 'row1'}],
        columns: [{name: 'col1', columnType: "STRING", key: 1}, {
          name: 'ref1',
          columnType: "REF",
          refColumns: ['firstName', 'lastName']
        }, {name: 'ref_arr1', columnType: "REF_ARRAY", refColumns: ['firstName', 'lastName']}]
        ,
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
