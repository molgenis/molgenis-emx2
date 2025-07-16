<script setup lang="ts">
import { rowToString } from "../../../utils/rowToString";
import type {
  columnValueObject,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import { computed, ref } from "vue";
import Button from "../../../components/Button.vue";
import { rowToSections } from "../../../utils/rowToSections";

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

const expanded = ref<boolean>(false);

function toLabel(row: columnValueObject) {
  return rowToString(row, props.refLabel ?? "");
}

function expandRow() {
  expanded.value = !expanded.value;
  emits("expand", props.refData);
}

const sections = computed(() =>
  rowToSections(props.refData, props.refMetadata)
);
</script>

<template>
  <li class="py-5 px-[30px] transition-all duration-500 overflow-hidden">
    <div
      @click="expandRow"
      class="flex items-center justify-between group hover:cursor-pointer"
    >
      <h3 class="text-title-contrast font-bold group-hover:underline">
        {{ toLabel(refData) }}
      </h3>

      <div class="flex items-center gap-4">
        <div class="flex items-center gap-2" v-if="props.canEdit">
          <Button
            :icon-only="true"
            icon="trash"
            type="inline"
            label="Remove"
            @click.stop="$emit('remove', refData)"
          ></Button>
          <!-- <Button not yet implemented
            :icon-only="true"
            icon="copy"
            type="inline"
            label="Duplicate"
            @click.stop="$emit('duplicate', refData)"
          ></Button> -->
          <Button
            class="hover:bg-gray-200 rounded-full"
            :icon-only="true"
            icon="edit"
            type="inline"
            label="Edit"
            @click.stop="$emit('edit', refData)"
          ></Button>
        </div>
        <div class="flex items-center gap-2">
          <Button
            :icon-only="true"
            :icon="expanded ? 'caret-up' : 'caret-down'"
            type="inline"
            label="Details"
            @click.stop="expandRow"
          ></Button>
        </div>
      </div>
    </div>
    <div
      class="transition-all duration-500 overflow-hidden"
      :class="expanded ? 'max-h-96 opacity-100' : 'max-h-0 opacity-0'"
    >
      <div class="mt-1" @click="$event.stopPropagation()">
        <ContentEMX2Section
          v-for="section in sections"
          :section="section"
        ></ContentEMX2Section>
      </div>
    </div>
  </li>
</template>
