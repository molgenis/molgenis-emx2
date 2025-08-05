<script setup lang="ts">
import { ref, computed } from "vue";
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

const expanded = ref<boolean>(false);

function toLabel(row: columnValueObject) {
  return rowToString(row, props.refLabel ?? "");
}

function expandRow() {
  expanded.value = !expanded.value;
  emits("expand", props.refData);
}
</script>

<template>
  <li
    :id="`input-refback-item-${label}`"
    class="py-5 px-[30px] transition-all duration-500 overflow-hidden bg-input"
  >
    <div
      @click="expandRow"
      class="flex items-center justify-between group hover:cursor-pointer"
    >
      <button
        :id="`input-refback-item-${label}-btn-label-expand`"
        :aria-controls="`input-refback-item-${label}-content-record`"
        :aria-expanded="expanded"
        :aria-haspopup="true"
        class="text-title-contrast font-bold group-hover:underline"
      >
        {{ label }}
      </button>

      <div class="flex items-center gap-4">
        <div
          class="flex items-center gap-2 text-button-text"
          v-if="props.canEdit"
        >
          <Button
            :id="`input-refback-item-${label}-btn-remove`"
            class="hover:bg-button-secondary-hover"
            :icon-only="true"
            :aria-controls="`input-refback-item-${label}-content-record`"
            :aria-expanded="expanded"
            :aria-haspopup="true"
            icon="trash"
            type="inline"
            label="Remove"
            @click.stop="$emit('remove', refData)"
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
            :aria-expanded="expanded"
            :aria-haspopup="true"
            icon="edit"
            type="inline"
            label="Edit"
            @click.stop="$emit('edit', refData)"
          />
        </div>
        <div class="flex items-center gap-2 text-button-text">
          <Button
            :id="`input-refback-item-${label}-btn-remove`"
            class="hover:bg-button-secondary-hover"
            :icon-only="true"
            :aria-controls="`input-refback-item-${label}-content-record`"
            :aria-expanded="expanded"
            :aria-haspopup="true"
            :icon="expanded ? 'caret-up' : 'caret-down'"
            type="inline"
            label="Details"
            @click.stop="expandRow"
          ></Button>
        </div>
      </div>
    </div>
    <div
      :id="`input-refback-item-${label}-content-record`"
      class="transition-all duration-500 overflow-hidden"
      :class="expanded ? 'max-h-96 opacity-100' : 'max-h-0 opacity-0'"
    >
      <div class="mt-1" @click="$event.stopPropagation()">
        <DisplayRecord
          :table-metadata="refMetadata"
          :input-row-data="refData"
        />
      </div>
    </div>
  </li>
</template>
