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
        <td
            v-for="col in columns"
            :key="col"
            @click="onRowClick(row)"
            style="cursor: pointer"
        >
          <ul v-if="Array.isArray(row[col])" class="list-unstyled">
            <div v-for="(item, index3) in row[col]" :key="index3">
              <li v-if="col === 'Diseases'">
                <a :href="item" target="_blank">link to disease</a>
              </li>
              <li v-if="col === 'ClinVar'">
                <p v-if="item === ''">No ClinVar match</p>
                <a v-else :href="'http://www.ncbi.nlm.nih.gov/clinvar/?term=' + item + '[alleleid]' + item" target="_blank">link to ClinVar</a>
              </li>
              <li v-else>
              {{ item }}
              </li>
            </div>
          </ul>
          <span v-else-if="row[col]">{{ flattenObject(row[col]) }} 54321</span>
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
Example
```
<template>
  <div>
    <TableSimple
        v-model="selectedItems"
        selectColumn="lastName"
        :defaultValue="selectedItems"
        :columns="['firstName','lastName','tags']"
        :rows="[{'firstName':'Donald','lastName':'Duck'},{'firstName':'Scrooge','lastName':'McDuck','tags':['blue','green']}]"
        @select="select"
        @deselect="deselect"
    />
    SelectedItems: {{ selectedItems }}
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
    <TableSimple
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
    data() {
      return {
        selectedItems: null
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
Example with only rowheader
```
<template>
  <div>
    <TableSimple
        v-model="selectedItems"
        :defaultValue="['Duck']"
        :columns="['firstName','lastName','tags']"
        :rows="[{'firstName':'Donald','lastName':'Duck'},{'firstName':'Scrooge','lastName':'McDuck','tags':['blue','green']}]"
        @click="click"
    >
      <template v-slot:rowheader="slotProps">
        my row header with props {{ JSON.stringify(slotProps) }}
      </template>
    </TableSimple>
  </div>
</template>
<script>
  export default {
    data() {
      return {
        selectedItems: null
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
