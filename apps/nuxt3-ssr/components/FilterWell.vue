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
    if (filter?.columnType === "_SEARCH") {
      clearSearch(filter);
    } else {
      clearConditions(filter);
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
  if (filter?.columnType !== "_SEARCH" && filter?.conditions.length === 0) {
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
    <div class="p-4 whitespace-nowrap justify-self-start self-start">
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

        <VDropdown
          :triggers="['hover', 'focus']"
          :distance="12"
          theme="tooltip"
        >
          <Button
            v-if="filter?.columnType !== '_SEARCH' && isFilterSet(filter)"
            @click="clearConditions(filter)"
            icon="trash"
            icon-position="right"
            size="tiny"
            type="filterWell"
          >
            {{ filter?.title }}
            <small class="text-gray-600">
              {{ `- ${filter?.conditions.length}` }}
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
