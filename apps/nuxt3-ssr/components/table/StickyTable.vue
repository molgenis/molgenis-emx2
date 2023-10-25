<template>
  <div role="region">
    <table class="table-auto">
      <thead>
        <tr>
          <th class="sticky left-0 top-0 z-30 bg-white"></th>
          <th
            v-for="(column, index) of columns"
            :key="`head-${index}`"
            class="sticky top-0 z-20 bg-white text-left align-bottom"
          >
            <div class="rotated-title">
              <span>
                <slot name="column" :value="column">{{ column }}</slot>
              </span>
            </div>
          </th>
        </tr>
      </thead>
      <tbody class="relative">
        <tr
          v-for="(row, rowIndex) of rows"
          :key="`tr-${rowIndex}`"
          class="text-left"
        >
          <th class="sticky left-0 z-10 bg-white whitespace-nowrap">
            <slot name="row" :value="{ row, rowIndex }">{{ row }}</slot>
          </th>
          <td
            class="relative bg-white whitespace-nowrap"
            v-for="(column, columnIndex) of columns"
            :key="`td-${columnIndex}`"
          >
            <slot name="cell" :value="{ row, column, rowIndex, columnIndex }">
            </slot>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: "TableStickyHeaders",
  props: {
    columns: Array,
    rows: Array,
  },
};
</script>

<style scoped>
/* 
    Based on: 
    https://css-tricks.com/a-table-with-both-a-sticky-header-and-a-sticky-first-column/
  */
</style>
