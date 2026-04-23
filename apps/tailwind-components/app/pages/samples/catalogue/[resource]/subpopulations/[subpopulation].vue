<template>
  <div>
    <BreadCrumbs :crumbs="breadcrumbs" align="center" />
    <DetailView
      schema-id="catalogue-demo"
      table-id="Subpopulations"
      :row-id="rowId"
      :show-side-nav="true"
      :expand-level="3"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import BreadCrumbs from "../../../../../components/BreadCrumbs.vue";
import DetailView from "../../../../../components/display/DetailView.vue";

const route = useRoute();

const breadcrumbs = computed(() => [
  { label: "Collections", url: "/samples/catalogue/LifeCycle" },
  {
    label: (route.params.resource as string) || "",
    url: `/samples/catalogue/${route.params.resource}`,
  },
  { label: (route.params.subpopulation as string) || "", url: "" },
]);

const rowId = computed(() => ({
  resource: { id: route.params.resource as string },
  name: route.params.subpopulation as string,
}));
</script>
