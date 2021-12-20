<template>
  <table class="table">
    <thead>
      <tr>
        <th
          v-for="(column, columnIndex) in columns"
          :key="columnIndex"
          scope="col"
        >
          {{ column.label }}
        </th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(row, index) in rows" :key="index">
        <td v-for="(column, columnIndex) in columns" :key="columnIndex">
          {{ row[column.name] }}
        </td>
      </tr>
    </tbody>
  </table>
</template>

<style></style>

<script>
export default {
  name: "TableDisplay",
  props: {
    /**
     * List of column objects, each object has a label and name
     */
    columns: {
      type: Array,
      default: () => [],
    },
    /**
     * List of row objects, each row is a key value object, where the keys refer to the column.name property
     */
    rows: {
      type: Array,
      default: () => [],
    },
  },
};
</script>

<docs>
### Table Display

Takes a list of columns and rows

 - Each column object has a label and name
 - Each row is a key-value-object, where the keys refers to the column.name property

```
const columns = [
  {name: 'firstName', label: 'First name'},
  {name: 'lastName', label: 'Sir name'},
  {name: 'occupation', label: 'Occupation'}
]

const rows = [
  {firstName: 'Albus', lastName: 'Dumbledore', occupation: 'Headmaster'},
  {firstName: 'Rubeus', occupation: 'Grounds keeper'},
  {lastName: 'Snape', occupation: 'Professor'}
]
<template>
  <table-display :columns=columns :rows=rows></table-display>
</template>


```
</docs>
