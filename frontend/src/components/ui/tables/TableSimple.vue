<template>
  <div class="table-responsive">
    <table
      class="table table-bordered table-condensed"
      :class="{ 'table-hover': selectColumn }"
    >
      <thead>
        <tr>
          <th v-if="hasColheader" scope="col" style="width: 1px;">
            <slot name="colheader" />
          </th>
          <th v-for="col in columns" :key="col" scope="col">
            <b>{{ col }}</b>
          </th>
        </tr>
      </thead>
      <tr v-for="row in rows" :key="JSON.stringify(row)">
        <td v-if="hasColheader">
          <slot name="rowheader" :row="row" />
          <input
            v-if="selectColumn"
            :checked="isSelected(row)"
            type="checkbox"
            @click="onRowClick(row)"
          >
        </td>
        <td
          v-for="col in columns"
          :key="col"
          style="cursor: pointer;"
          @click="onRowClick(row)"
        >
          <ul v-if="Array.isArray(row[col])" class="list-unstyled">
            <li v-for="(item, index3) in row[col]" :key="index3">
              {{ item }}
            </li>
          </ul>
          <span v-else-if="row[col]">{{ flattenObject(row[col]) }}</span>
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
  emits: ['update:modelValue'],
  data: function() {
    return {
      selectedItems: [],
    }
  },
  computed: {
    hasColheader() {
      return (
        this.selectColumn ||
        !!this.$slots['colheader'] ||
        !!this.$slots['colheader'] ||
        !!this.$slots['rowheader'] ||
        !!this.$slots['rowheader']
      )
    },
  },
  watch: {
    selectedItems() {
      this.$emit('update:modelValue', this.selectedItems)
    },
  },
  created() {
    if (this.defaultValue instanceof Array) {
      this.selectedItems = this.defaultValue
    } else {
      this.selectedItems.push(this.defaultValue)
    }
  },
  methods: {
    flattenObject(object) {
      let result = ''
      if (typeof object === 'object') {
        Object.keys(object).forEach((key) => {
          if (object[key] === null) {
            // nothing
          } else if (typeof object[key] === 'object') {
            result += this.flattenObject(object[key])
          } else {
            result += ' ' + object[key]
          }
        })
      } else {
        result = object
      }
      return result.replace(/^\./, '')
    },
    isSelected(row) {
      return (
        this.selectedItems != null &&
        this.selectedItems.includes(row[this.selectColumn])
      )
    },
    onRowClick(row) {
      if (this.selectColumn) {
        if (this.isSelected(row)) {
          /** when a row is deselected */
          this.selectedItems = this.selectedItems.filter(
            (item) => item !== row[this.selectColumn],
          )
          this.$emit('deselect', row[this.selectColumn])
        } else {
          /** when a row is selected */
          this.selectedItems.push(row[this.selectColumn])
          this.$emit('select', row[this.selectColumn])
        }
      } else {
        this.$emit('click', row)
      }
    },
  },
}
</script>
