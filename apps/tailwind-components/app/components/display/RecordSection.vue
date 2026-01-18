<script setup lang="ts">
import { computed } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import type { ISectionColumn } from "../../../types/types";
import DefinitionList from "../DefinitionList.vue";
import DefinitionListTerm from "../DefinitionListTerm.vue";
import DefinitionListDefinition from "../DefinitionListDefinition.vue";
import RecordColumn from "./RecordColumn.vue";

const props = withDefaults(
  defineProps<{
    heading?: IColumn | null;
    isSection?: boolean;
    columns: ISectionColumn[];
    showEmpty?: boolean;
    getRefClickAction?: (col: IColumn, row: IRow) => () => void;
  }>(),
  {
    heading: null,
    isSection: false,
    showEmpty: false,
  }
);

const headingClasses = computed(() =>
  props.isSection ? "text-2xl font-bold mb-4" : "text-xl font-semibold mb-3"
);

const visibleColumns = computed(() => {
  if (props.showEmpty) return props.columns;
  return props.columns.filter((col) => {
    const val = col.value;
    if (val === null || val === undefined || val === "") return false;
    if (Array.isArray(val) && val.length === 0) return false;
    return true;
  });
});

function isListColumn(col: ISectionColumn): boolean {
  const type = col.meta.columnType;
  return type === "REF_ARRAY" || type === "REFBACK";
}

const regularColumns = computed(() =>
  visibleColumns.value.filter((col) => !isListColumn(col))
);

const listColumns = computed(() =>
  visibleColumns.value.filter((col) => isListColumn(col))
);
</script>

<template>
  <section class="mb-6">
    <h2 v-if="heading" :class="headingClasses">
      {{ heading.label || heading.id }}
    </h2>
    <p
      v-if="heading?.description"
      class="text-gray-600 dark:text-gray-400 mb-4"
    >
      {{ heading.description }}
    </p>

    <!-- Regular columns in definition list grid -->
    <DefinitionList v-if="regularColumns.length">
      <template v-for="col in regularColumns" :key="col.meta.id">
        <DefinitionListTerm>{{
          col.meta.label || col.meta.id
        }}</DefinitionListTerm>
        <DefinitionListDefinition>
          <RecordColumn
            :column="col.meta"
            :value="col.value"
            :show-empty="showEmpty"
            :get-ref-click-action="getRefClickAction"
          />
        </DefinitionListDefinition>
      </template>
    </DefinitionList>

    <!-- List columns (REF_ARRAY/REFBACK) rendered full-width with label above -->
    <div v-if="listColumns.length" class="mt-4 space-y-4">
      <div
        v-for="col in listColumns"
        :key="col.meta.id"
        class="record-list-section"
      >
        <dt class="font-bold text-body-base mb-2">
          {{ col.meta.label || col.meta.id }}
        </dt>
        <dd>
          <RecordColumn
            :column="col.meta"
            :value="col.value"
            :show-empty="showEmpty"
            :get-ref-click-action="getRefClickAction"
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
