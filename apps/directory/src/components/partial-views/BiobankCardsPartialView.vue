<template>
  <div class="border-bottom p-3">
    <div v-if="!loading && foundBiobanks > 0">
      <div class="d-flex mb-4 justify-content-between">
        <result-header v-if="!loading" class="w-25" />

        <pagination class="align-self-center" />
        <!-- Alignment block -->
        <div class="w-25"></div>
      </div>

      <div
        class="d-flex justify-content-center flex-wrap biobank-cards-container"
      >
        <biobank-card
          v-for="biobank in biobanksShown"
          :key="biobank.id || biobank"
          :biobank="biobank"
          :fullSize="biobanksShown.length === 1"
        >
        </biobank-card>
      </div>
      <pagination class="mt-4" />
    </div>
    <div v-else-if="!loading && foundBiobanks === 0" class="status-text">
      <h4>No biobanks were found</h4>
    </div>
    <div v-else class="status-text">
      <h4>
        Loading data...

        {{ settingsStore.pageSize }}
        <i class="fa fa-spinner fa-pulse" aria-hidden="true"></i>
      </h4>
    </div>
  </div>
</template>

<script>
import { useBiobanksStore } from "../../stores/biobanksStore";
import { useSettingsStore } from "../../stores/settingsStore";
export default {
  setup() {
    const biobanksStore = useBiobanksStore();
    const settingsStore = useSettingsStore();
    return { biobanksStore, settingsStore };
  },
  data() {
    return {
      biobanks: [],
    };
  },
  async mounted() {
    this.biobanks = await this.biobanksStore.getBiobanks();
  },
};
</script>
