<template>
  <p class="pb-3 text-title">
    Default breadcrumbs, with 3 items, aligns middle
  </p>
  <div>
    <BreadCrumbs :crumbs="crumbs" />
  </div>

  <p class="pt-6 pb-3 text-title">Breadcrumbs aligned left</p>
  <div>
    <BreadCrumbs :crumbs="crumbs" align="left" />
  </div>

  <p class="pt-6 pb-3 text-title">Breadcrumbs with 'item 4' as current</p>
  <div>
    <BreadCrumbs :crumbs="crumbs" current="item 4" align="left" />
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
interface Crumbs {
  [key: string]: string;
}

const crumbs = ref<Crumbs>({});
crumbs.value["item 1"] = route.path;
crumbs.value["item 2"] = route.path;
crumbs.value["item 3"] = route.path;
</script>
