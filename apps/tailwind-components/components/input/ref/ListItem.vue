<script setup lang="ts">
import { rowToString } from "../../../utils/rowToString";
import type { columnValueObject } from "../../../../metadata-utils/src/types";
import { ref } from "vue";
import Button from "~/components/Button.vue";

const props = withDefaults(
  defineProps<{
    refData: columnValueObject;
    refLabel?: string;
    canEdit?: boolean;
  }>(),
  {
    canEdit: true,
  }
);

const expanded = ref<boolean>(false);

function toLabel(row: columnValueObject) {
  return rowToString(row, props.refLabel ?? "");
}
</script>

<template>
  <li class="py-5 px-[30px] transition-all duration-500 overflow-hidden">
    <div class="flex items-center justify-between">
      <h3 class="text-title-contrast font-bold">{{ toLabel(refData) }}</h3>

      <div class="flex items-center gap-4">
        <div class="flex items-center gap-2" v-if="props.canEdit">
          <Button
            :icon-only="true"
            icon="trash"
            type="inline"
            label="Remove"
          ></Button>
          <Button
            :icon-only="true"
            icon="copy"
            type="inline"
            label="Duplicate"
          ></Button>
          <Button
            class="hover:bg-gray-200 rounded-full"
            :icon-only="true"
            icon="edit"
            type="inline"
            label="Edit"
          ></Button>
        </div>
        <div class="flex items-center gap-2">
          <Button
            :icon-only="true"
            :icon="expanded ? 'caret-up' : 'caret-down'"
            type="inline"
            label="Details"
            @click="expanded = !expanded"
          ></Button>
        </div>
      </div>
    </div>
    <div
      class="transition-all duration-500 overflow-hidden"
      :class="expanded ? 'max-h-96 opacity-100' : 'max-h-0 opacity-0'"
    >
      <div class="mt-4">
        This is the content inside the expanding div. You can put anything here
        — text, images, components.
      </div>
    </div>
  </li>
</template>
