<template>
  <div class="border-bottom p-3">
    <div v-if="biobanksStore.biobankCardsHaveResults">
      <div class="d-flex mb-4 justify-content-between">
        <result-header class="w-25" />
        {{ biobanksStore.biobankCardsBiobankCount }}
        {{ biobanksStore.biobankCardsCollectionCount }}
        {{ biobanksStore.biobankCardsSubcollectionCount }}
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
    <div v-else-if="!biobanksStore.waiting" class="status-text">
      <h4>No biobanks were found</h4>
    </div>
    <div v-else class="status-text">
      <h4>
        Loading data...
        <!-- TODO: add spinner -->
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
    this.biobanks = await this.biobanksStore.getBiobankCards();
  },
};
</script>
