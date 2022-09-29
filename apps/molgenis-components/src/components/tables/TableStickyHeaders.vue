<template>
  <div role="region" class="border border-light">
    <table>
      <thead>
        <tr>
          <th></th>
          <th v-for="(column, index) of columns" :key="`head-${index}`">
            <div class="rotated-title">
              <span>
                <slot name="column" :value="column">{{ column }}</slot>
              </span>
            </div>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, rowIndex) of rows" :key="`tr-${rowIndex}`">
          <th>
            <slot name="row" :value="row">{{ row }}</slot>
          </th>
          <td
            v-for="(column, columnIndex) of columns"
            :key="`td-${columnIndex}`"
          >
            <slot name="cell" :value="data[row][column]">
              {{ data[row][column] }}
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
    data: Object,
  },
};
</script>

<style scoped>
/* 
  Based on: 
  https://css-tricks.com/a-table-with-both-a-sticky-header-and-a-sticky-first-column/
*/

table td {
  text-align: right;
  border: 1px solid var(--light);
}
.rotated-title {
  width: 2em;
  height: 10em;
  vertical-align: bottom;
  position: relative;
}
.rotated-title > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 12em;
  height: 2em;
  transform-origin: 0 0;
  transform: rotate(-55deg) translate(-7.5em, 5.25em);
  display: inline-block;
  z-index: 1;
  position: relative;
}
table {
  border-spacing: 0;
}
table thead th {
  padding-bottom: 1em;
}
table thead tr:after {
  display: inline-block;
  content: "";
  width: 100%;
  height: 15px;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.05) 0%,
    rgba(0, 0, 0, 0) 100%
  );
  position: absolute;
  left: 0;
  bottom: -15px;
  pointer-events: none;
}
table thead th:first-child {
  background: white;
  z-index: 1;
  border-bottom: 0px;
}
table thead {
  position: sticky;
  top: 0;
  z-index: 1;
  background: white;
}
table tbody {
  position: relative;
}
table tbody tr:hover {
  background-color: var(--light);
}
table tbody tr:hover th {
  background-color: var(--light);
}
table tbody td,
table tbody th {
  position: relative;
  white-space: nowrap;
}
table tbody td:hover::before {
  content: "";
  position: absolute;
  display: inline-block;
  background-color: var(--light);
  left: 0;
  right: 0;
  top: -100vh;
  bottom: -100vh;
  z-index: -1;
}
table thead th:first-child {
  position: sticky;
  left: 0;
  z-index: 2;
}
table thead th:first-child::after {
  display: inline-block;
  content: "";
  width: 15px;
  height: 100vh;
  background: linear-gradient(
    90deg,
    rgba(0, 0, 0, 0.05) 0%,
    rgba(0, 0, 0, 0) 100%
  );
  position: absolute;
  right: -15px;
  top: 0;
  pointer-events: none;
}
table tbody th {
  position: sticky;
  left: 0;
  background: white;
  z-index: 1;
  padding-right: 1em;
}
[role="region"] {
  width: 100%;
  max-height: 98vh;
  overflow: auto;
}
</style>

<docs>
<template>
  <div>
    <DemoItem>
      <TableStickyHeaders
        :columns="columns"
        :rows="rows"
        :data="data"
      >
        <template #column="columnProps">
           {{ columnProps.value }}
        </template>
        <template #row="rowProps">
           {{ rowProps.value }}
        </template>
        <template #cell="cell">
          <div v-if="cell.value===0" class='text-center text-black-50'>-</div>
          <div v-else-if="cell.value<10">﹤10</div>
          <div v-else>{{cell.value}}</div>
        </template>
      </TableStickyHeaders>
    </DemoItem>
  </div>
</template>

<script>
export default {
  data() {
    return generateTestData(50)
  },
};
function generateTestData(items){
  const columns=[];
  const rows=[];
  const data={};
  for(var x=0;x<items;x++){
    rows.push(`${x}`);
    columns.push(`${x}`);
    const datarow={};
    for(var y=0;y<items;y++){
      datarow[`${y}`]=parseInt(`${x}${y}`);
    }
    data[`${x}`]=datarow;
  }
  return {columns,rows,data}
}
</script>

</docs>
