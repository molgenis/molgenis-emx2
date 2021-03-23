<template>
  <div v-if="countFilters > 0" class="from-inline">
    {{ countFilters }} filter{{ countFilters > 1 ? "s" : "" }} (<a
      href="#"
      @click.prevent="removeAll"
    >remove all filters</a>) :<br>
    <span v-for="(column, idx) in filters" :key="column.name">
      <span v-for="(value, idx2) in column.conditions" :key="idx2">
        <span v-if="Array.isArray(value)">
          <FilterWell
            v-if="value[0] !== null && value[1] !== null"
            :label="value[0] + ' &lt; ' + column.name + ' &lt; ' + value[1]"
            @click="remove(idx, idx2)"
          />
          <FilterWell
            v-else-if="value[0] !== null"
            :label="value[0] + ' &lt; ' + column.name"
            @click="remove(idx, idx2)"
          />

          <FilterWell
            v-else-if="value[1] !== null"
            :label="column.name + ' &lt; ' + value[1]"
            @click="remove(idx, idx2)"
          />
        </span>
        <span v-else>
          <FilterWell
            :label="column.name + ' = ' + renderValue(value)"
            @click="remove(idx, idx2)"
          />
        </span>
      </span>
    </span>
  </div>
</template>

<script>
import FilterWell from './FilterWell.vue'

export default {
  components: {
    FilterWell,
  },
  props: {
    /** two-way bindable array of column metadata. Will add 'conditions' property to hold filter values */
    filters: Array,
  },
  emits: ['update:filters'],
  computed: {
    countFilters() {
      let count = 0
      if (Array.isArray(this.filters)) {
        this.filters.forEach((column) => {
          if (Array.isArray(column.conditions)) {
            column.conditions.forEach((condition) => {
              if (Array.isArray(condition)) {
                if (condition[0] !== null || condition[1] != null) {
                  count++
                }
              } else {
                if (condition != null) {
                  count++
                }
              }
            })
          }
        })
      }
      return count
    },
  },
  methods: {
    flattenObject(object) {
      let result = ''
      Object.keys(object).forEach((key) => {
        if (object[key] === null) {
          // nothing
        } else if (typeof object[key] === 'object') {
          result += this.flattenObject(object[key])
        } else {
          result += ' ' + object[key]
        }
      })
      return result
    },
    remove(idx, idx2) {
      let update = this.filters
      update[idx].conditions.splice(idx2, 1)
      this.$emit('update:filters', update)
    },
    removeAll() {
      let update = this.filters
      for (var idx in update) {
        if (Array.isArray(update[idx].conditions)) {
          update[idx].conditions = []
        }
      }
      this.$emit('update:filters', update)
    },
    renderValue(value) {
      if (typeof value === 'object' && value !== null) {
        return this.flattenObject(value)
      } else {
        return value
      }
    },
  },
}
</script>
