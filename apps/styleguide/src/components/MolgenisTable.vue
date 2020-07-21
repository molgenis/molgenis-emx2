<template>
  <div class="table-responsive">
    <table
      class="table table-bordered table-condensed"
      :class="{ 'table-hover': select }"
    >
      <thead>
        <th scope="col" style="width: 1px;">
          <slot name="colheader" />
        </th>
        <th v-for="col in metadata.columns" :key="col.name" scope="col">
          <b>{{ col.name }}</b>
        </th>
      </thead>
      <tbody>
        <tr v-for="(row, idx) in data" :key="JSON.stringify(row)">
          <td>
            <div style="display: flex;">
              <div class="form-check form-check-inline mr-1">
                <input
                  v-if="select"
                  type="checkbox"
                  class="form-check-input position-static"
                  :checked="isSelected(row)"
                  @click="onRowClick(row)"
                />
              </div>
              <slot name="rowheader" :row="row" />
            </div>
          </td>
          <td
            v-for="col in metadata.columns"
            :key="idx + col.name"
            @click="onRowClick(row)"
            style="cursor: pointer"
          >
            <div
              v-for="(value, idx2) in renderValue(row, col)"
              :key="idx + col + idx2"
            >
              {{ value }}
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    {{ JSON.stringify(selectedItems) }}
    {{ JSON.stringify(metadata) }}
  </div>
</template>

<script>
export default {
  props: {
    select: {
      type: Boolean,
      default: true
    },
    metadata: Object,
    data: Array
  },
  data: () => {
    return {
      selectedItems: []
    };
  },
  methods: {
    renderValue(row, col) {
      if (row[col.name] === undefined) {
        return [];
      }
      if (col.columnType == "REF_ARRAY" || col.columnType == "REFBACK") {
        return row[col.name].map(v => col.refColumns.map(r => v[r]).join(" "));
      } else if (col.columnType == "REF") {
        return [col.refColumns.map(r => row[col.name][r]).join(" ")];
      } else {
        return [row[col.name]];
      }
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
    }
  },
  watch: {
    selectedItems() {
      this.$emit("input", this.selectedItems);
    }
  }
};
</script>

<style>
th,
td {
  text-align: left;
}
</style>

<docs>
    example
    ```
    <template>
        <div>
            <MolgenisTable v-model="selected" :metadata="metadata" :data="data" @select="click" @deselect="click"
                           @click="click">
                <template v-slot:rowheader="slotProps">
                    <RowButtonEdit
                            table="Pet"
                            :pkey="{name:'spike'}"
                            @close="reload"
                    />
                    <RowButtonDelete
                            table="Pet"
                            :pkey="{name:'spike'}"
                            @close="reload"
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
