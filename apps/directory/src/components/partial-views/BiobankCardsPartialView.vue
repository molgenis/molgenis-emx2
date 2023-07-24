<template>
  <div class="border-bottom pt-3">
    <div v-if="biobanksStore.biobankCardsHaveResults">
      <div class="d-flex mb-4 justify-content-between">
        <result-header class="w-25" />
        <pagination-bar class="align-self-center" />
        <!-- Alignment block -->
        <div class="w-25"></div>
      </div>
      <div
        class="d-flex justify-content-center flex-wrap biobank-cards-container"
      >
        <biobank-card
          :style="`width:${cardWidth}px;`"
          v-for="biobank in biobanksShown"
          :key="biobank.id"
          :biobank="biobank"
        >
        </biobank-card>
      </div>
      <pagination-bar class="mt-4" />
    </div>
    <div v-else-if="!biobanksStore.waiting" class="status-text">
      <h4>{{ noResultsText }}</h4>
    </div>
    <div v-else class="status-text text-center">
      <spinner class="mt-2" />
    </div>
  </div>
</template>

<script>
import { useBiobanksStore } from "../../stores/biobanksStore";
import { useSettingsStore } from "../../stores/settingsStore";
import ResultHeader from "../biobankcards-components/ResultHeader.vue";
import PaginationBar from "../biobankcards-components/PaginationBar.vue";
import BiobankCard from "../biobankcards-components/BiobankCard.vue";
import { Spinner } from "../../../../molgenis-components";
import { useFiltersStore } from "../../stores/filtersStore";

export default {
  setup() {
    const biobanksStore = useBiobanksStore();
    const settingsStore = useSettingsStore();
    const filtersStore = useFiltersStore();

    return { biobanksStore, settingsStore, filtersStore };
  },
  components: {
    ResultHeader,
    PaginationBar,
    BiobankCard,
    Spinner,
  },
  computed: {
    biobanksShown() {
      if (this.biobanksStore.waiting) return [];
      return this.biobanksStore.biobankCards.slice(
        this.settingsStore.config.pageSize *
          (this.settingsStore.currentPage - 1),
        this.settingsStore.config.pageSize * this.settingsStore.currentPage
      );
    },
    noResultsText() {
      return this.filtersStore.bookmarkWaitingForApplication
        ? "Loading state"
        : "No biobanks were found";
    },
  },
  watch: {
    biobanksShown() {
      this.calculateCardWidth();
    },
  },
  methods: {
    calculateCardWidth() {
      const mainviewElements = document.getElementsByClassName("main-view");

      if (!mainviewElements || !mainviewElements.length) return;

      const mainView = mainviewElements[0].clientWidth;

      if (this.biobanksShown.length === 1) {
        this.cardWidth = mainView;
      } else {
        const cardWidthPixels = 25 * 16;

        let cardsShown = 0;
        let remainderMainPixels = mainView;
        while (remainderMainPixels >= cardWidthPixels) {
          remainderMainPixels = remainderMainPixels - cardWidthPixels;
          cardsShown++;
        }

        const cardWidth = mainView / cardsShown - (cardsShown === 2 ? 16 : 22);

        this.cardWidth = cardWidth;
      }
    },
  },
  data() {
    return {
      cardWidth: 25 * 16,
    };
  },
  created() {
    window.addEventListener("resize", this.calculateCardWidth);
  },
  destroyed() {
    window.removeEventListener("resize", this.calculateCardWidth);
  },
  async mounted() {
    await this.biobanksStore.getBiobankCards();
    this.calculateCardWidth();
  },
};
</script>

<style>
.biobank-cards-container {
  gap: 2rem;
}

.status-text {
  text-align: center;
  justify-content: center;
  padding: 1rem;
}
</style>
