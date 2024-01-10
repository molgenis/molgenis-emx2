<script setup lang="ts">
import type {
  IFilter,
  IConditionsFilter,
  ISearchFilter,
} from "~~/interfaces/types";

const props = defineProps<{
  filters: IFilter[];
}>();
const { filters } = toRefs(props);

function clearSearch(filter: ISearchFilter) {
  filter.config.search = "";
}

function clearConditions(filter: IConditionsFilter) {
  filter.conditions = [];
}

function clearAll() {
  filters.value.forEach((filter) => {
    if (filter.config.type === "SEARCH") {
      clearSearch(filter as ISearchFilter);
    } else {
      clearConditions(filter as IConditionsFilter);
    }
  });
}

function isFilterSet(filter: IFilter) {
  if (
    filter.config.type === "SEARCH" &&
    (filter.search === undefined || filter.search === "")
  ) {
    return false;
  }
  if (
    filter.config.type !== "SEARCH" &&
    filter.conditions &&
    filter.conditions.length === 0
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
      <template v-for="filter in filters">
        <Button
          v-if="filter.config.type === 'SEARCH' && isFilterSet(filter)"
          @click="clearSearch(filter as ISearchFilter)"
          icon="trash"
          icon-position="right"
          size="tiny"
          type="filterWell"
        >
          {{ `${filter.config.label}: ${filter?.search}` }}
        </Button>

        <VDropdown
          :triggers="['hover', 'focus']"
          :distance="12"
          theme="tooltip"
        >
          <Button
            v-if="filter.config.type !== 'SEARCH' && isFilterSet(filter)"
            @click="clearConditions(filter as IConditionsFilter)"
            icon="trash"
            icon-position="right"
            size="tiny"
            type="filterWell"
          >
            {{ filter.config.label }}
            <small class="text-gray-600">
              {{ `- ${filter.conditions?.length ?? 0}` }}
            </small>
          </Button>
          <template #popper>
            <ul style="list-style-type: disc" class="pl-3 min-w-95">
              <li v-for="item in filter?.conditions">{{ item.name }}</li>
            </ul>
          </template>
        </VDropdown>
      </template>
      <Button
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
