<template>
  <div role="region relative">
    <table class="table-auto relative z-0">
      <thead>
        <tr>
          <th class="sticky left-0 top-0 z-30 bg-white">
            <div class="absolute inset-0 border-l border-b"></div>
          </th>
          <th
            v-for="(column, index) of columns"
            :key="`head-${index}`"
            class="sticky top-0 z-20 bg-white text-left align-bottom"
          >
            <div class="absolute inset-0 border-l border-b"></div>
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
            <div class="absolute inset-0 border-b"></div>

            <slot name="row" :value="{ row, rowIndex }">{{ row }}</slot>
          </th>
          <td
            class="relative whitespace-nowrap vertical-hover-bar border-b"
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
    <div
      class="flex items-center justify-items-end absolute z-100 right-0 inset-y-0 w-10 bg-gradient-to-r from-transparent to-white pointer-events-none transition-opacity"
    ></div>
    <div
      class="flex items-center justify-items-end absolute z-100 bottom-0 inset-x-0 h-10 bg-gradient-to-b from-transparent to-white pointer-events-none transition-opacity"
    ></div>
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
table.table-auto tbody tr:hover th {
  background-color: rgb(244, 244, 244);
}
.vertical-hover-bar:hover::before {
  z-index: -1;
  content: "";
  position: absolute;
  pointer-events: none;
  left: 0;
  right: 0;
  top: -99999px;
  bottom: -99999px;
  background-color: rgb(244, 244, 244);
  border: 1px solid rgb(226, 226, 226);
}

th::before {
  z-index: -1;
  content: "";
  position: absolute;
  pointer-events: none;
  top: 0;
  bottom: 0;
  width: 1rem;
  right: -1rem;
  background-image: linear-gradient(to right, rgba(0, 0, 0, 0.1), transparent);
}
</style>
