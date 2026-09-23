<script setup lang="ts">
import Table from "../Table.vue";
const props = defineProps<{
  columns: { id: string; label: string }[];
  rows: Record<string, unknown>[];
}>();
</script>

<template>
  <Table>
    <template #head>
      <TableHeadRow>
        <TableHead v-for="column in columns" :key="column.id">
          {{ column.label }}
        </TableHead>
      </TableHeadRow>
    </template>
    <template #body>
      <TableRow v-for="row in rows" :key="`${row.schemaId}/${row.roleName}`">
        <TableCell v-for="column in columns" :key="column.id">
          {{ row[column.id] }}
        </TableCell>
      </TableRow>
    </template>
    <template #foot> </template>
  </Table>
</template>
