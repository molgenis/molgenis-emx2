<template>
  <span v-if="column.refTable">
    {{ column.columnType.toLowerCase() }}({{
      column.refSchema ? column.refSchema + "." : ""
    }}{{ column.refTable
    }}<span v-if="column.refBack">, refBack={{ column.refBack }}</span>
    <span v-if="column.refLink">, refLink={{ column.refLink }}</span
    >)
  </span>
  <span v-else>
    {{ column.columnType.toLowerCase() }}
  </span>
  <span v-if="column.required === true || column.required === 'true'">
    required
  </span>
  <span v-if="column.readonly === true || column.readonly === 'true'">
    readonly
  </span>
  <span v-if="column.defaultValue">
    defaultValue='{{ column.defaultValue }}'
  </span>
  <span v-if="column.refLabel"> refLabel='{{ column.refLabel }}' </span>
  <span v-if="column.computed"> computed="{{ column.computed }}"</span>
</template>

<script>
export default {
  props: {
    column: Object,
  },
};
</script>
