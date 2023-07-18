<template>
  <main>
    <section class="d-flex justify-content-center">
      <landingpage-header
        :header-text="pageHeader"
        :linkText="gotoCatalogue"
        :css="landingpageCss"
      >
        <landingpage-search
          :buttonText="search.buttonText"
          :ariaLabel="search.ariaLabel"
          :searchPlaceholder="search.searchPlaceholder"
          :css="landingpageCss"
        />
        <button
          class="edit-button header-section"
          @click="$emit('open', 'landingpage-header')"
          v-if="editable"
        >
          Edit
        </button>
      </landingpage-header>
    </section>
    <section
      class="d-flex justify-content-between mx-5 my-5 cta-container w-75 mx-auto"
    >
      <button
        class="edit-button cta-section"
        @click="$emit('open', 'landingpage-ctas')"
        v-if="editable"
      >
        Edit
      </button>
      <landingpage-call-to-action
        :key="index + cta.ctaText"
        v-for="(cta, index) in callToActions"
        :ctaUrl="cta.ctaUrl"
        :ctaText="cta.ctaText"
        :bodyHtml="cta.bodyHtml"
        :css="landingpageCss"
      />
    </section>
    <section class="d-flex justify-content-between mx-auto mb-5 w-75">
      <landingpage-biobank-spotlight
        :headerText="biobankSpotlight.header"
        :biobankName="biobankSpotlight.biobankName"
        :biobankId="biobankSpotlight.biobankId"
        :bodyHtml="biobankSpotlight.bodyHtml"
        :buttonText="biobankSpotlight.buttonText"
        :css="landingpageCss"
      />
      <button
        class="edit-button biobank-spotlight-section"
        @click="$emit('open', 'landingpage-biobank-spotlight')"
        v-if="editable"
      >
        Edit
      </button>
      <landingpage-collection-spotlight
        :headerText="collectionSpotlight.header"
        :collections="collectionSpotlight.collections"
        :css="landingpageCss"
      />
      <button
        class="edit-button collection-spotlight-section"
        @click="$emit('open', 'landingpage-collection-spotlight')"
        v-if="editable"
      >
        Edit
      </button>
    </section>
  </main>
</template>

<script>
import LandingpageBiobankSpotlight from "../components/landingpage-components/LandingpageBiobankSpotlight.vue";
import LandingpageCallToAction from "../components/landingpage-components/LandingpageCallToAction.vue";
import LandingpageHeader from "../components/landingpage-components/LandingpageHeader.vue";
import LandingpageCollectionSpotlight from "../components/landingpage-components/LandingpageCollectionSpotlight.vue";
import LandingpageSearch from "../components/landingpage-components/LandingpageSearch.vue";
import { mapState } from "vuex";

export default {
  props: {
    editable: {
      type: Boolean,
      required: false,
      default: () => false,
    },
  },
  components: {
    LandingpageHeader,
    LandingpageSearch,
    LandingpageCallToAction,
    LandingpageBiobankSpotlight,
    LandingpageCollectionSpotlight,
  },
  computed: {
    ...mapState(["landingpage"]),
    pageHeader() {
      return this.landingpage.page_header;
    },
    gotoCatalogue() {
      return this.landingpage.goto_catalogue_link;
    },
    search() {
      return this.landingpage.page_search;
    },
    biobankSpotlight() {
      return this.landingpage.page_biobank_spotlight;
    },
    collectionSpotlight() {
      return this.landingpage.page_collection_spotlight;
    },
    callToActions() {
      return this.landingpage.page_call_to_actions;
    },
    landingpageCss() {
      return this.landingpage.css;
    },
  },
};
</script>

<style scoped>
section {
  position: relative;
}

.edit-button {
  position: absolute;
  width: 3rem;
  top: 5%;
  z-index: 1000;
}

.header-section {
  right: 14%;
}

.cta-section {
  right: 2%;
}
.biobank-spotlight-section {
  top: 2%;
  right: 52%;
}

.collection-spotlight-section {
  top: 2%;
  right: 2%;
}
</style>
