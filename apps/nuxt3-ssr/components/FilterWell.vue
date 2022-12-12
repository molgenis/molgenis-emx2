<script setup lang="ts">
const { filters } = withDefaults(
  defineProps<{
    filters: [];
  }>(),
  {}
);
</script>

<template>
  <div>
    <div
      class="bg-search-results-view-tabs text-white flex items-center rounded-t-3px p-4"
    >
      <div class="pr-4">Active filters</div>
      <div>
        <Button
          icon="trash"
          icon-position="right"
          size="small"
          type="filterWell"
        >
          filter
        </Button>
      </div>
    </div>
    <!--{{ filters }}-->
  </div>
</template>
