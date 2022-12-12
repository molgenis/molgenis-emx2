<script setup lang="ts">
const props = defineProps<{
  filters: [];
}>();
const { filters } = toRefs(props);
/*
watch(filters, () => {
  console.log(filters);
});
*/
</script>

<template>
  <div
    class="bg-search-results-view-tabs text-white flex items-center rounded-t-3px"
  >
    <div class="p-4 whitespace-nowrap">Active filters</div>
    <div class="grid grid-cols-3 gap-4 content-around p-4">
      <template v-for="filter in filters">
        <Button
          v-if="
            filter?.columnType === '_SEARCH' &&
            filter?.search !== undefined &&
            filter?.search !== ''
          "
          icon="trash"
          icon-position="right"
          size="small"
          type="filterWell"
        >
          {{ `${filter?.title}: ${filter?.search}` }}
        </Button>
        <Button
          v-if="
            filter?.columnType === 'ONTOLOGY' && filter?.conditions.length > 0
          "
          icon="trash"
          icon-position="right"
          size="small"
          type="filterWell"
        >
          {{ `${filter?.title}`
          }}<small class="text-white bg-blue-500 rounded-full p-1 ml-3">
            {{ filter?.conditions.length }}
          </small>
        </Button>
      </template>
      <Button icon="trash" icon-position="right" size="small" type="filterWell"
        >Remove all
      </Button>
    </div>
  </div>
</template>
