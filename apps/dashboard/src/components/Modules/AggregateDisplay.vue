<template>
  <div>
    <h1>Aggregate</h1>
    <AggregateTable
      tableName="Pet"
      schemaName="pet store"
      :minimumValue="1"
      :allColumns="allColumns"
    />
  </div>
</template>

<script setup>
import { ref } from "vue";
import { AggregateTable } from "molgenis-components";
let allColumns = [
  {
    name: "name",
    columnType: "STRING",
  },
  {
    name: "category",
    columnType: "REF",
  },
  {
    name: "tags",
    columnType: "ONTOLOGY_ARRAY",
  },
  {
    name: "orders",
    columnType: "REFBACK",
  },
];
</script>

<style></style>
