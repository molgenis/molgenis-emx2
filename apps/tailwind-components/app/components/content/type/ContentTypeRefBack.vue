<script setup lang="ts">
import { computed } from "vue";
import type { ISectionField } from "../../../../types/types";
import type { columnValueObject } from "../../../../../metadata-utils/src/types";
import { isEmptyValue } from "../../../../app/utils/displayUtils";

const props = defineProps<{
  field: ISectionField;
}>();

const items = computed(() => props.field.value as columnValueObject[]);

const longestRowIndex = computed(() => {
  return items.value?.reduce(
    (maxIndex: number, row: columnValueObject, currentIndex: number) =>
      Object.keys(row).length > Object.keys(maxIndex).length
        ? currentIndex
        : maxIndex,
    0
  );
});

const rows = computed(() => {
  return items.value?.map((row: columnValueObject) => {
    return Object.entries(row).map(([key, value]) => {
      const filteredValue =
        typeof value === "object"
          ? isEmptyValue(value)
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
