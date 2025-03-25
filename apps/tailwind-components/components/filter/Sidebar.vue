<script setup lang="ts">
import type { IFilter, IOntologyFilter, IRefArrayFilter } from "~/types/types";
import type {
  columnValue,
  columnValueObject,
} from "../../../metadata-utils/src/types";
const props = withDefaults(
  defineProps<{
    id: string;
    title: string;
    filters: IFilter[];
    mobileDisplay?: boolean;
  }>(),
  {
    mobileDisplay: false,
  }
);

const emit = defineEmits(["update:filters"]);

function handleFilterUpdate(filter: IFilter) {
  const index = props.filters.findIndex((f: IFilter) => f.id === filter.id);
  const newFilters = [...props.filters];
  newFilters[index] = filter;
  emit("update:filters", newFilters);
}
</script>

<template>
  <div
    class="mt-7.5 rounded-t-3px rounded-b-50px"
    :class="{ 'bg-sidebar-gradient': !mobileDisplay }"
  >
    <h2
      v-if="!mobileDisplay"
      class="p-5 uppercase font-display text-heading-3xl text-search-filter-title"
    >
      {{ title }}
    </h2>

    <template v-if="filters">
      <FilterContainer
        v-for="filter in filters"
        :title="filter.config.label"
        :conditions="(filter as IOntologyFilter | IRefArrayFilter).conditions ? (filter as IOntologyFilter | IRefArrayFilter).conditions : []"
        @update:conditions="(value) => {(filter as IOntologyFilter).conditions = value; handleFilterUpdate(filter)}"
        :search="filter.search"
        @update:search="
          (value) => {
            filter.search = value;
            handleFilterUpdate(filter);
          }
        "
        :mobileDisplay="mobileDisplay"
        :initialCollapsed="filter.config.initialCollapsed"
      >
        <FilterSearch
          v-if="filter.config.type === 'SEARCH'"
          :mobileDisplay="mobileDisplay"
          :model-value="filter.search || ''"
          @update:model-value="
            (value) => {
              filter.search = value;
              handleFilterUpdate(filter);
            }
          "
        />
        <InputOntology
          v-else-if="filter.config.type === 'ONTOLOGY'"
          :id="id + '-' + filter.id"
          :is-array="true"
          :ontologySchemaId="filter.config.ontologySchema"
          :ontologyTableId="filter.config.ontologyTableId"
          :filter="filter.config.filter"
          :mobileDisplay="mobileDisplay"
          :filterLabel="filter.config.label"
          :model-value="(filter as IOntologyFilter).conditions as string[]"
          @update:model-value="(value: columnValue) => {(filter as IOntologyFilter).conditions = value; handleFilterUpdate(filter)}"
        />
        <InputRef
          v-else-if="filter.config.type === 'REF_ARRAY'"
          :id="id + '-' + filter.id"
          :isArray="true"
          :refTableId="filter.config.refTableId"
          :refSchemaId="filter.config.refSchemaId"
          :refLabel="filter.config.refLabel"
          :filter="filter.config.filter"
          :mobileDisplay="mobileDisplay"
          :refDescription="filter.config.refDescription"
          :model-value="(filter as IRefArrayFilter).conditions as columnValueObject[]"
          @update:model-value="(value: columnValueObject[]) => {(filter as IRefArrayFilter).conditions = value; handleFilterUpdate(filter)}"
        />
      </FilterContainer>
    </template>

    <slot v-else></slot>

    <hr class="mx-5 border-black opacity-10" />

    <!-- <div class="flex items-center p-5">
      <button class="flex items-center">
        <BaseIcon name="plus" class="text-search-filter-expand" :width="18" />
        <span
          class="ml-3 text-search-filter-expand text-body-base hover:underline"
        >
          More filters
        </span>
      </button>
      <CustomTooltip
        label="More"
        hoverColor="white"
        content="tooltip"
        class="ml-3"
      />
    </div> -->
  </div>
</template>
