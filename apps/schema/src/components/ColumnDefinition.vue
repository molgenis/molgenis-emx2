<template>
  <span v-if="column.key">key={{ column.key }}</span>
  <span v-if="column.refTableName">
    {{ column.columnType.toLowerCase() }}({{
      column.refSchemaName ? column.refSchemaName + "." : ""
    }}{{ column.refTableName
    }}<span v-if="column.refBackName">, refBack={{ column.refBackName }}</span>
    <span v-if="column.refLinkName">, refLink={{ column.refLinkName }}</span
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
