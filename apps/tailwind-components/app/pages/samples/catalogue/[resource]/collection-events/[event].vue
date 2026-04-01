<template>
  <div>
    <DetailView
      schema-id="catalogue-demo"
      table-id="CollectionEvents"
      :row-id="rowId"
      :show-side-nav="true"
      :expand-level="3"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import DetailView from "../../../../../components/display/DetailView.vue";

const route = useRoute();
const rowId = computed(() => ({
  resource: { id: route.params.resource as string },
  name: route.params.event as string,
}));
</script>
