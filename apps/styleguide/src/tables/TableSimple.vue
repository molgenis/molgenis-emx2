<template>
  <div class="table-responsive">
    <table
      class="table table-bordered table-condensed"
      :class="{ 'table-hover': selectColumn }"
    >
      <thead>
        <tr>
          <th scope="col" style="width: 1px">
            <slot name="colheader" />
          </th>
          <th v-for="col in columns" :key="col" scope="col">
            <b>{{ col }}</b>
          </th>
        </tr>
      </thead>
      <tr v-for="row in rows" :key="JSON.stringify(row)">
        <td>
          <slot name="rowheader" :row="row" />
          <input
            v-if="selectColumn"
            type="checkbox"
            :checked="isSelected(row)"
            @click="onRowClick(row)"
          />
        </td>
        <td
          v-for="col in columns"
          :key="col"
          @click="onRowClick(row)"
          style="cursor: pointer"
        >
          <ul v-if="Array.isArray(row[col])" class="list-unstyled">
            <li v-for="(item, index3) in row[col]" :key="index3">{{ item }}</li>
          </ul>
          <span v-else>{{ row[col] }}</span>
        </td>
      </tr>
    </table>
  </div>
</template>

<script>
/** Data table. Has also option to have row selection. Selection events must be handled outside this view. */
export default {
  props: {
    /** the column names */
    columns: Array,
    /** list of rows matching column names */
    rows: Array,
    /** set to create select boxes that will yield this columns value when selected. */
    selectColumn: String,
    /** default value */
    defaultValue: Array,
  },
  data: function () {
    return {
      selectedItems: [],
    };
  },
  watch: {
    selectedItems() {
      this.$emit("input", this.selectedItems);
    },
  },
  created() {
    if (this.defaultValue instanceof Array) {
      this.selectedItems = this.defaultValue;
    } else {
      this.selectedItems.push(this.defaultValue);
    }
  },
  methods: {
    isSelected(row) {
      return (
        this.selectedItems != null &&
        this.selectedItems.includes(row[this.selectColumn])
      );
    },
    onRowClick(row) {
      if (this.selectColumn) {
        if (this.isSelected(row)) {
          /** when a row is deselected */
          this.selectedItems = this.selectedItems.filter(
            (item) => item !== row[this.selectColumn]
          );
          this.$emit("deselect", row[this.selectColumn]);
        } else {
          /** when a row is selected */
          this.selectedItems.push(row[this.selectColumn]);
          this.$emit("select", row[this.selectColumn]);
        }
      } else {
        this.$emit("click", row);
      }
    },
  },
};
</script>

<style scoped>
th,
td {
  text-align: left;
}
</style>

<docs>
    Example
    ```
    <template>
        <div>
            <DataTable
                    v-model="selectedItems"
                    selectColumn="lastName"
                    :defaultValue="selectedItems"
                    :columns="['firstName','lastName','tags']"
                    :rows="[{'firstName':'Donald','lastName':'Duck'},{'firstName':'Scrooge','lastName':'McDuck','tags':['blue','green']}]"
                    @select="select"
                    @deselect="deselect"
            />
            SelectedItems: {{selectedItems}}
        </div>
    </template>
    <script>
        export default {
            data: function () {
                return {
                    selectedItems: ['Duck']
                };
            },
            methods: {
                select(value) {
                    alert("select " + value);
                },
                deselect(value) {
                    alert("deselect " + value);
                }
            }
        };
    </script>
    ```
    Example using simple click (no selection)
    ```
    <template>
        <div>
            <DataTable
                    v-model="selectedItems"
                    :defaultValue="['Duck']"
                    :columns="['firstName','lastName','tags']"
                    :rows="[{'firstName':'Donald','lastName':'Duck'},{'firstName':'Scrooge','lastName':'McDuck','tags':['blue','green']}]"
                    @click="click"
            />
        </div>
    </template>
    <script>
        export default {
            methods: {
                click(value) {
                    alert("click " + JSON.stringify(value));
                }
            }
        };
    </script>
    ```
</docs>
