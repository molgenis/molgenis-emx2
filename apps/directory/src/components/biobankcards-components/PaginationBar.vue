<template>
  <Pagination
    v-if="
      biobanksStore.biobankCardsBiobankCount > settingsStore.config.pageSize &&
      !biobanksStore.waiting
    "
    class="justify-content-center mb-2"
    :count="biobanksStore.biobankCardsBiobankCount"
    v-model="settingsStore.currentPage"
    :limit="settingsStore.config.pageSize"
  />
</template>

<script>
import { Pagination } from "molgenis-components";
import { useSettingsStore } from "../../stores/settingsStore";
import { useBiobanksStore } from "../../stores/biobanksStore";

export default {
  setup() {
    const settingsStore = useSettingsStore();
    const biobanksStore = useBiobanksStore();
    return { settingsStore, biobanksStore };
  },
  components: {
    Pagination,
  },
};
</script>
