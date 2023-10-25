<template>
  <div role="region">
    <table class="table-auto relative z-0">
      <thead>
        <tr>
          <th class="sticky left-0 top-0 z-30 bg-white"></th>
          <th
            v-for="(column, index) of columns"
            :key="`head-${index}`"
            class="sticky top-0 z-20 bg-white text-left align-bottom"
          >
            <div class="absolute inset-0 border-l"></div>
            <div class="rotated-title">
              <span>
                <slot name="column" :value="column">{{ column }}</slot>
              </span>
            </div>
          </th>
        </tr>
      </thead>
      <tbody class="relative z-0">
        <tr
          v-for="(row, rowIndex) of rows"
          :key="`tr-${rowIndex}`"
          class="text-left hover:bg-gray-100"
        >
          <th class="sticky left-0 z-10 bg-white whitespace-nowrap">
            <div class="absolute inset-0 border-t"></div>

            <slot name="row" :value="{ row, rowIndex }">{{ row }}</slot>
          </th>
          <td
            class="relative whitespace-nowrap vertical-hover-bar border-t"
            v-for="(column, columnIndex) of columns"
            :key="`td-${columnIndex}`"
          >
            <div class="z-10">
              <slot name="cell" :value="{ row, column, rowIndex, columnIndex }">
              </slot>
            </div>
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
    Sticky table Based on: 
    https://css-tricks.com/a-table-with-both-a-sticky-header-and-a-sticky-first-column/
*/
table.table-auto tbody tr:hover th {
  background-color: rgb(244, 244, 244);
}
.vertical-hover-bar:hover::before {
  z-index: -1;
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  top: -99999px;
  bottom: -99999px;
  background-color: rgb(244, 244, 244);
  border: 1px solid rgb(226, 226, 226);
}
</style>
