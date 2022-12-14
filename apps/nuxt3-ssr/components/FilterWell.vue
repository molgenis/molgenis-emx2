<script setup lang="ts">
const props = defineProps<{
  filters: [];
}>();
const { filters } = toRefs(props);

function clearSearch(filter) {
  filter.search = "";
}

function clearConditions(filter) {
  filter.conditions = [];
}

function clearAll() {
  filters.value.forEach((filter) => {
    if (filter?.columnType === "ONTOLOGY") {
      clearConditions(filter);
    }
    if (filter?.columnType === "_SEARCH") {
      clearSearch(filter);
    }
  });
}

function isFilterSet(filter) {
  if (
    filter?.columnType === "_SEARCH" &&
    (filter?.search == undefined || filter?.search == "")
  ) {
    return false;
  }
  if (filter?.columnType === "ONTOLOGY" && filter?.conditions.length === 0) {
    return false;
  }
  return true;
}

function isAFilterSet(filters) {
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
    <div class="p-3 whitespace-nowrap justify-self-start self-start">
      Active filters
    </div>
    <div class="flex flex-wrap gap-3 content-around p-3">
      <template v-for="filter in filters">
        <Button
          v-if="filter?.columnType === '_SEARCH' && isFilterSet(filter)"
          @click="clearSearch(filter)"
          icon="trash"
          icon-position="right"
          size="tiny"
          type="filterWell"
        >
          {{ `${filter?.title}: ${filter?.search}` }}
        </Button>
        <Button
          v-if="filter?.columnType === 'ONTOLOGY' && isFilterSet(filter)"
          @click="clearConditions(filter)"
          v-tooltip="filter?.conditions.map((item) => item.name).join(', ')"
          icon="trash"
          icon-position="right"
          size="tiny"
          type="filterWell"
        >
          {{ `${filter?.title}`
          }}<small class="text-white bg-blue-500 rounded-full p-1 ml-2">
            {{ filter?.conditions.length }}
          </small>
        </Button>
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
