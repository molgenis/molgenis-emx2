<template>
  <table class="table" :class="{ 'table-hover': isClickable }">
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
      <tr
        v-for="(row, index) in rows"
        :key="index"
        @click="$emit('row-click', row)"
      >
        <td v-for="(column, columnIndex) in columns" :key="columnIndex">
          {{ row[column.name] }}
        </td>
      </tr>
    </tbody>
  </table>
</template>

<style>
.table-hover tbody tr:hover {
  cursor: pointer;
}
</style>

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
    isClickable: {
      type: Boolean,
      default: () => false,
    },
  },
};
</script>

<docs>
<template>
  <div>
    <label>Table Display</label>
    <p>
      Takes a list of columns and rows

      - Each column object has a label and name
      - Each row is a key-value-object, where the keys refers to the column.name property
    </p>
    <table-display :columns="[
  {name: 'firstName', label: 'First name'},
  {name: 'lastName', label: 'Sir name'},
  {name: 'occupation', label: 'Occupation'}
]" :rows=" [
  {firstName: 'Albus', lastName: 'Dumbledore', occupation: 'Headmaster'},
  {firstName: 'Rubeus', occupation: 'Grounds keeper'},
  {lastName: 'Snape', occupation: 'Professor'}
]">

    </table-display>
    <label>Set isClickable to make the rows actionable</label>
    <table-display :columns="[
    {name: 'a', label: 'A'},
    {name: 'b', label: 'B'},
    {name: 'c', label: 'C'}
    ]" :rows="[
    {a: 'Click'},
    {b: 'On'},
    {c: 'Me'}
    ]" :isClickable="true"
                   @row-click="alert(JSON.stringify($event))"></table-display>
  </div>
</template>
<script>
  export default {
    methods: {
      alert(string) {
        alert(string);
      }
    }
  }
</script>
</docs>
