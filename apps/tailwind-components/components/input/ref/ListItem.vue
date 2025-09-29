<script setup lang="ts">
import { computed } from "vue";
import { rowToString } from "../../../utils/rowToString";
import { Button, DisplayRecord } from "#components";

import type {
  columnValueObject,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<{
    refData: columnValueObject;
    refMetadata: ITableMetaData;
    refLabel?: string;
    canEdit?: boolean;
  }>(),
  {
    canEdit: true,
  }
);

const emits = defineEmits<{
  (e: "remove", row: columnValueObject): void;
  (e: "duplicate", row: columnValueObject): void;
  (e: "edit", row: columnValueObject): void;
  (e: "expand", row: columnValueObject): void;
}>();

const label = computed<string | undefined>(() => {
  if (props.refData) {
    return toLabel(props.refData);
  }
});

function toLabel(row: columnValueObject) {
  return rowToString(row, props.refLabel ?? "");
}
</script>

<template>
  <li :id="`input-refback-item-${label}`">
    <Accordion
      :label="(label as string)"
      :open-by-default="false"
      @click="emits('expand', props.refData)"
    >
      <template #toolbar>
        <div
          class="flex items-center gap-2 text-button-text"
          v-if="props.canEdit"
        >
          <Button
            :id="`input-refback-item-${label}-btn-remove`"
            class="hover:bg-button-secondary-hover"
            :icon-only="true"
            :aria-haspopup="true"
            icon="trash"
            type="inline"
            label="Remove"
            @click.stop="$emit('remove', refData)"
          />
          <!-- <Button not yet implemented
            :icon-only="true"
            icon="copy"
            type="inline"
            label="Duplicate"
            @click.stop="$emit('duplicate', refData)"
          ></Button> -->
          <Button
            :id="`input-refback-item-${label}-btn-remove`"
            class="hover:bg-button-secondary-hover"
            :icon-only="true"
            :aria-controls="`input-refback-item-${label}-content-record`"
            :aria-haspopup="true"
            icon="edit"
            type="inline"
            label="Edit"
            @click.stop="$emit('edit', refData)"
          />
        </div>
      </template>
      <div @click="$event.stopPropagation()">
        <DisplayRecord
          :table-metadata="refMetadata"
          :input-row-data="refData"
        />
      </div>
    </Accordion>
  </li>
</template>
