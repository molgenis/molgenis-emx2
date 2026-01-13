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

  <p class="pt-6 pb-3 text-title">
    Breadcrumbs with a number in one of the labels
  </p>
  <div>
    <BreadCrumbs
      :crumbs="crumbsWithNumberLabels"
      current="item 4"
      align="left"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRoute } from "vue-router";
import BreadCrumbs from "../components/BreadCrumbs.vue";
import type { Crumb } from "../../types/types";

const route = useRoute();

const crumbs = ref<Crumb[]>([]);
crumbs.value.push({ label: "item 1", url: route.path });
crumbs.value.push({ label: "item 2", url: route.path });
crumbs.value.push({ label: "item 3", url: route.path });

const crumbsWithNumberLabels = ref<Crumb[]>([]);
crumbsWithNumberLabels.value.push({ label: "1", url: route.path });
crumbsWithNumberLabels.value.push({
  label: "non number label",
  url: route.path,
});
crumbsWithNumberLabels.value.push({ label: "3", url: route.path });
</script>
