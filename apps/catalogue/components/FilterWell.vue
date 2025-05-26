<script setup lang="ts">
import { useId } from "vue";
import type {
  IFilter,
  IConditionsFilter,
  ISearchFilter,
  IOntologyFilter,
} from "~~/interfaces/types";

const ariaId = useId();
const props = defineProps<{
  filters: IFilter[];
}>();

const emit = defineEmits(["update:filters"]);

function handleFilerUpdate(filter: IFilter) {
  const index = props.filters.findIndex((f: IFilter) => f.id === filter.id);
  const newFilters = [...props.filters];
  newFilters[index] = filter;
  emit("update:filters", newFilters);
}

function clearAll() {
  const cleared = props.filters.map((filter) => {
    if (filter.config.type === "SEARCH") {
      (filter as ISearchFilter).search = "";
    } else {
      (filter as IConditionsFilter).conditions = [];
    }
    return filter;
  });
  emit("update:filters", cleared);
}

function isFilterSet(filter: IFilter) {
  if (
    filter.config.type === "SEARCH" &&
    (filter?.search === undefined || filter?.search === "")
  ) {
    return false;
  }
  if (
    filter.config.type !== "SEARCH" &&
    (filter as IConditionsFilter).conditions &&
    (filter as IConditionsFilter).conditions.length === 0
  ) {
    return false;
  }
  return true;
}

function isAFilterSet(filters: IFilter[]) {
  return filters.some((filter) => {
    return isFilterSet(filter);
  });
}
</script>

<template>
  <div
    v-if="isAFilterSet(filters)"
    class="bg-search-results-view-tabs text-white flex items-center rounded-t-3px"
  >
    <div class="p-4 whitespace-nowrap justify-self-start self-start">
      Active filters
    </div>
    <div class="flex flex-wrap gap-3 content-around p-3">
      <template v-for="(filter, index) in filters">
        <Button
          v-if="filter.config.type === 'SEARCH' && isFilterSet(filter)"
          @click="
            () => {
              filter.search = '';
              handleFilerUpdate(filter);
            }
          "
          icon="trash"
          icon-position="right"
          size="tiny"
          type="filterWell"
        >
          {{ `${filter.config.label}: ${filter?.search}` }}
        </Button>

        <VDropdown
          :aria-id="ariaId + '_' + index"
          :triggers="['hover', 'focus']"
          :distance="12"
          theme="tooltip"
        >
          <Button
            v-if="filter.config.type !== 'SEARCH' && isFilterSet(filter)"
            @click="
              () => {
                if((filter as IOntologyFilter).conditions) {
                  (filter as IOntologyFilter).conditions = [];
                }
                handleFilerUpdate(filter);
              }
            "
            icon="trash"
            icon-position="right"
            size="tiny"
            type="filterWell"
          >
            {{ filter.config.label }}
            <small class="text-gray-600">
              {{ `- ${(filter as IConditionsFilter).conditions.length}` }}
            </small>
          </Button>
          <template
            #popper
            v-if="filter.config.type !== 'SEARCH' && isFilterSet(filter)"
          >
            <ul style="list-style-type: disc" class="pl-3 min-w-95">
              <li v-for="item in (filter as IConditionsFilter)?.conditions">
                {{ item.name }}
              </li>
            </ul>
          </template>
        </VDropdown>
      </template>
      <Button
        id="fiter-well-clear-all"
        icon="trash"
        icon-position="right"
        size="tiny"
        type="filterWell"
        @click="clearAll"
        >Remove all
      </Button>
    </div>
  </div>
</template>
