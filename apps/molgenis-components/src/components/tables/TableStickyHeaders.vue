<template>
  <div role="region" class="border border-light">
    <table class="table table-sm bg-white table-bordered table-hover">
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
          <td v-for="(column, columnIndex) of columns" :key="`td-${columnIndex}`">
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
table {
  width: auto;
  border-collapse: separate;
  border-spacing: 0;
  border: none;
}

table td {
  text-align: right;
  padding: 0.1rem 0.8rem;
}

.rotated-title {
  padding-top: 8rem;
  vertical-align: bottom;
  position: relative;
}

.rotated-title > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 8rem;
  height: 2rem;
  transform-origin: 0 100%;
  transform: rotate(-90deg) translate(0, 2px);
  display: inline-block;
  z-index: 2;
  position: absolute;
  bottom: 0;
  left: 100%;
  line-height: 2.2rem;
}

table thead {
  position: sticky;
  top: 0;
  z-index: 2;
  background: white;
}

table tbody {
  position: relative;
}

table thead th {
  border-left: 0px;
}

table thead th:first-child {
  position: sticky;
  left: 0;
  background-color: white;
  border-left: 1px solid #dee2e6;
  z-index: 3;
}

table tbody td {
  border-left: none;
  border-top: none;
}

table tbody td,
table tbody th {
  position: relative;
  white-space: nowrap;
  background-color: white;
  border-top: none;
}

table tbody td:nth-child(2) {
  padding-left: 1.5rem;
  border-left: none;
}

table tbody tr:first-child td,
table tbody tr:first-child th {
  padding-top: 1.5rem;
  border-top: none;
}

table tbody th {
  position: sticky;
  left: 0;
  z-index: 1;
  padding-right: 1rem;
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
