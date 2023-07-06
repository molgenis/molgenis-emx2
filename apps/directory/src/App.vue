<script setup>
import { Molgenis } from "molgenis-components";
import { computed, onMounted, watch } from "vue";
import { applyBookmark } from "./functions/bookmarkMapper";
import { useRoute } from "vue-router";

onMounted(async () => {
  await applyBookmark();
});

const route = useRoute();

const query = computed(() => route.query);

watch(
  query,
  (newQuery) => {
    console.log(newQuery)
    applyBookmark(newQuery);
  },
  { immediate: true, deep: true }
);
</script>

<template>
  <molgenis>
    <RouterView @click="closeAllDropdownButtons" />
  </molgenis>
</template>
<script>
export default {
  methods: {
    closeAllDropdownButtons(event) {
      const allDropdownButtons = document.querySelectorAll(".dropdown-button");

      for (const dropdownButton of allDropdownButtons) {
        if (dropdownButton.id !== event.target.id) {
          dropdownButton.removeAttribute("open");
        }
      }
    },
  },
};
</script>

<style>
/* removing the built-in nav because it conflicts */
nav[aria-label="breadcrumb"]:not(.directory-nav) {
  display: none;
}
</style>
