<script setup lang="ts">
import { rowToString } from "~/utils/rowToString";
import type { columnValueObject } from "../../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<{
    data: columnValueObject;
    refLabel: string;
    canEdit?: boolean;
  }>(),
  {
    canEdit: true,
  }
);

function toLabel(row: columnValueObject) {
  return rowToString(row, props.refLabel);
}
</script>

<template>
  <li class="py-5 px-[30px]">
    <div class="flex items-center justify-between">
      <h3 class="text-title-contrast font-bold">{{ toLabel(data) }}</h3>
      <div class="flex items-center gap-4">
        <div class="flex items-center gap-2" v-if="props.canEdit">
          <Button
            :icon-only="true"
            icon="trash"
            type="outline"
            size="small"
          ></Button>
          <Button
            :icon-only="true"
            icon="copy"
            type="outline"
            size="small"
          ></Button>
          <Button
            :icon-only="true"
            icon="edit"
            type="outline"
            size="small"
          ></Button>
        </div>
        <div class="flex items-center gap-2">
          <Button
            :icon-only="true"
            icon="caret-down"
            type="outline"
            size="small"
          ></Button>
        </div>
      </div>
    </div>
  </li>
</template>
