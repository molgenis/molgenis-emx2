<script setup lang="ts">
import { computed } from "vue";
import type { ISectionField } from "../../../types/types";
import type { IColumn } from "../../../../metadata-utils/src/types";
import { isEmptyValue, isTopSection } from "../../utils/displayUtils";
import DefinitionList from "../DefinitionList.vue";
import DefinitionListTerm from "../DefinitionListTerm.vue";
import DefinitionListDefinition from "../DefinitionListDefinition.vue";
import RecordColumn from "./DetailColumn.vue";

const props = withDefaults(
  defineProps<{
    heading?: IColumn | null;
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
  return props.columns.filter((col) => !isEmptyValue(col.value));
});

function isListColumn(col: ISectionField): boolean {
  const type = col.meta.columnType;
  if (type === "REF_ARRAY" || type === "REFBACK") return true;
  return false;
}

const regularColumns = computed(() =>
  visibleColumns.value.filter((col) => !isListColumn(col))
);

const listColumns = computed(() =>
  visibleColumns.value.filter((col) => isListColumn(col))
);

const sectionHeading = computed(() => {
  if (!props.heading || isTopSection(props.heading)) return false;
  return props.heading.label || props.heading.id;
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
        <DefinitionListTerm class="capitalize">{{
          col.meta.label || col.meta.id
        }}</DefinitionListTerm>
        <DefinitionListDefinition class="text-black">
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
        <dt class="font-bold text-body-base mb-2 capitalize">
          {{ col.meta.label || col.meta.id }}
        </dt>
        <dd class="text-black ml-0">
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
