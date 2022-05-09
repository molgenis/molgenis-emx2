<template>
  <div class="table-responsive">
    <table class="table table-bordered" :class="{ 'table-hover': tableHover }">
      <thead>
        <tr>
          <th scope="col" style="width: 1px" v-if="hasColheader">
            <slot name="colheader" />
          </th>
          <th v-for="col in columns" :key="col" scope="col">
            {{ col }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="JSON.stringify(row)">
          <td v-if="hasColheader">
            <div style="display: flex">
              <slot name="rowheader" :row="row" />
              <input
                class="form-check form-check-inline mr-1"
                v-if="selectColumn"
                type="checkbox"
                :checked="isSelected(row)"
                @click="onRowClick(row)"
              />
            </div>
          </td>
          <td
            v-for="col in columns"
            :key="col"
            @click="onRowClick(row)"
            style="cursor: pointer"
          >
            <ul v-if="Array.isArray(row[col])" class="list-unstyled">
              <li v-for="(item, index3) in row[col]" :key="index3">
                {{ item }}
              </li>
            </ul>
            <span v-else-if="row[col]">{{ flattenObject(row[col]) }}</span>
          </td>
        </tr>
      </tbody>
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
  computed: {
    tableHover() {
      return this.selectColumn || (this.$listeners && this.$listeners.click);
    },
    hasColheader() {
      return (
        this.selectColumn ||
        !!this.$slots["colheader"] ||
        !!this.$scopedSlots["colheader"] ||
        !!this.$slots["rowheader"] ||
        !!this.$scopedSlots["rowheader"]
      );
    },
  },
  methods: {
    flattenObject(object) {
      let result = "";
      if (typeof object === "object") {
        Object.keys(object).forEach((key) => {
          if (object[key] === null) {
            //nothing
          } else if (typeof object[key] === "object") {
            result += this.flattenObject(object[key]);
          } else {
            result += " " + object[key];
          }
        });
      } else {
        result = object;
      }
      return result.replace(/^\./, "");
    },
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

<template>
  <div>
    <div>
      <TableSimple
        v-model="selectedItems1"
        selectColumn="lastName"
        :defaultValue="selectedItems1"
        :columns="['firstName', 'lastName', 'tags']"
        :rows="[
          { firstName: 'Donald', lastName: 'Duck' },
          { firstName: 'Scrooge', lastName: 'McDuck', tags: ['blue', 'green'] },
        ]"
        @select="select"
        @deselect="deselect"
      />
      SelectedItems: {{ selectedItems1 }}
    </div>

    <div>
      <TableSimple
        v-model="selectedItems2"
        :defaultValue="['Duck']"
        :columns="['firstName', 'lastName', 'tags']"
        :rows="[
          { firstName: 'Donald', lastName: 'Duck' },
          { firstName: 'Scrooge', lastName: 'McDuck', tags: ['blue', 'green'] },
        ]"
        @click="click"
      />
    </div>

    <div>
      <TableSimple
        v-model="selectedItems3"
        :defaultValue="['Duck']"
        :columns="['firstName', 'lastName', 'tags']"
        :rows="[
          { firstName: 'Donald', lastName: 'Duck' },
          { firstName: 'Scrooge', lastName: 'McDuck', tags: ['blue', 'green'] },
        ]"
        @click="click"
      >
        <template v-slot:rowheader="slotProps">
          my row header with props {{ JSON.stringify(slotProps) }}
        </template>
      </TableSimple>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      selectedItems1: ["Duck"],
      selectedItems2: null,
      selectedItems3: null,
    };
  },
  methods: {
    click(value) {
      alert("click " + JSON.stringify(value));
    },
    select(value) {
      alert("select " + value);
    },
    deselect(value) {
      alert("deselect " + value);
    },
  },
};
</script>

</docs>
