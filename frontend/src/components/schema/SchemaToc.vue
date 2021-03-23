<template>
  <div>
    <h1>Schema:</h1>
    <div v-if="tables">
      <p
        v-for="table in tables.filter((t) => t.externalSchema == undefined)"
        :key="table.name"
      >
        <a href=".">{{ table.name }}</a>
      </p>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    tables: Array,
  },
}
</script>
