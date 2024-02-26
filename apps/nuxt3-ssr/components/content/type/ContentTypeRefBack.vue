<script setup lang="ts">
import type { ISectionField } from "~/interfaces/types";
const props = defineProps<{
  field: ISectionField;
}>();

const longestRowIndex = computed(() => {
  return props.field.value.reduce(
    (maxIndex: number, row: Object, currentIndex: number) =>
      Object.keys(row).length > Object.keys(maxIndex).length
        ? currentIndex
        : maxIndex,
    0
  );
});

function isEmpty(obj: any) {
  for (const prop in obj) {
    if (Object.hasOwn(obj, prop)) {
      return false;
    }
  }

  return true;
}

const rows = computed(() => {
  return props.field.value.map((row: Object) => {
    return Object.entries(row).map(([key, value]) => {
      const filteredValue =
        typeof value === "object"
          ? isEmpty(value)
            ? ""
            : JSON.stringify(value)
          : value;
      return { key, value: filteredValue };
    });
  });
});
</script>

<template>
  <Table>
    <template #head>
      <TableHeadRow>
        <TableHead v-for="longestRowColumns in rows[longestRowIndex]">{{
          longestRowColumns.key
        }}</TableHead>
      </TableHeadRow>
    </template>
    <template #body>
      <TableRow v-for="row in rows">
        <TableCell v-for="longestRowColumns in rows[longestRowIndex]">{{
          row.find((col: any) => col.key === longestRowColumns.key)?.value
        }}</TableCell>
      </TableRow>
    </template>
  </Table>
</template>
