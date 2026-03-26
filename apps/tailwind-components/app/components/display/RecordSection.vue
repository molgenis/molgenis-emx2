<script setup lang="ts">
import { computed } from "vue";
import type { IColumnDisplay, ISectionField } from "../../../types/types";
import DefinitionList from "../DefinitionList.vue";
import DefinitionListTerm from "../DefinitionListTerm.vue";
import DefinitionListDefinition from "../DefinitionListDefinition.vue";
import RecordColumn from "./RecordColumn.vue";

const props = withDefaults(
  defineProps<{
    heading?: IColumnDisplay | null;
    isSection?: boolean;
    columns: ISectionField[];
    showEmpty?: boolean;
    schemaId?: string;
    parentRowId?: Record<string, any>;
  }>(),
  {
    heading: null,
    isSection: false,
    showEmpty: false,
  }
);

const headingClasses = "mb-5 uppercase text-heading-4xl font-display";

const visibleColumns = computed(() => {
  if (props.showEmpty) return props.columns;
  return props.columns.filter((col) => {
    const val = col.value;
    if (val === null || val === undefined || val === "") return false;
    if (Array.isArray(val) && val.length === 0) return false;
    return true;
  });
});

function isListColumn(col: ISectionField): boolean {
  const type = col.meta.columnType;
  return type === "REF_ARRAY" || type === "REFBACK";
}

const regularColumns = computed(() =>
  visibleColumns.value.filter((col) => !isListColumn(col))
);

const listColumns = computed(() =>
  visibleColumns.value.filter((col) => isListColumn(col))
);

const sectionHeading = computed(() => {
  if (
    !props.heading ||
    props.heading.id === "_top" ||
    props.heading.id === "mg_top_of_form" ||
    props.heading.label === "_top"
  )
    return false;
  const result = props.heading.label || props.heading.id;
  return result;
});
</script>

<template>
  <section
    :id="heading?.id"
    class="bg-content py-18 lg:px-12.5 px-5 text-title-contrast xl:rounded-3px last:rounded-b-50px shadow-primary xl:border-b-0 border-b-[1px] overflow-hidden"
  >
    <h2 v-if="sectionHeading" :class="headingClasses">
      {{ sectionHeading }}
    </h2>
    <p v-if="heading?.description" class="mb-5 prose max-w-none">
      {{ heading.description }}
    </p>

    <DefinitionList v-if="regularColumns.length">
      <template v-for="col in regularColumns" :key="col.meta.id">
        <DefinitionListTerm class="text-record-label">{{
          col.meta.displayLabel || col.meta.label || col.meta.id
        }}</DefinitionListTerm>
        <DefinitionListDefinition class="text-record-value">
          <RecordColumn
            :column="col.meta"
            :value="col.value"
            :show-empty="showEmpty"
            :schema-id="schemaId"
            :parent-row-id="parentRowId"
          />
        </DefinitionListDefinition>
      </template>
    </DefinitionList>

    <div v-if="listColumns.length" class="mt-4 space-y-4">
      <div
        v-for="col in listColumns"
        :key="col.meta.id"
        class="record-list-section"
      >
        <dt class="font-bold text-body-base mb-2 text-record-label">
          {{ col.meta.displayLabel || col.meta.label || col.meta.id }}
        </dt>
        <dd class="text-record-value">
          <RecordColumn
            :column="col.meta"
            :value="col.value"
            :show-empty="showEmpty"
            :schema-id="schemaId"
            :parent-row-id="parentRowId"
          />
        </dd>
      </div>
    </div>

    <p
      v-else-if="!visibleColumns.length && showEmpty"
      class="text-gray-400 italic"
    >
      No columns to display
    </p>
  </section>
</template>
