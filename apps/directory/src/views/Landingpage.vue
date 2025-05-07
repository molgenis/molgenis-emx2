<template>
  <main>
    <section class="w-75 mx-auto">
      <h1 class="text-center py-5">
        {{ headerText }}
      </h1>
      <div class="row">
        <div
          class="col-sm-6 col-md-4 col-xl-3 d-flex justify-content-center pb-3"
        >
          <ImageCard
            label="Autoimmune Diseases"
            image-src="img/bacterial-blue.png"
            alt-text="bacterial"
            :to="{ name: 'catalogue', query: { Categories: 'autoimmune' } }"
          />
        </div>
        <div
          class="col-sm-6 col-md-4 col-xl-3 d-flex justify-content-center pb-3"
        >
          <ImageCard
            label="Cardiovascular Deseases"
            image-src="img/heart-blue.png"
            alt-text="bacterial"
            :to="{ name: 'catalogue', query: { Categories: 'cardiovascular' } }"
          />
        </div>
        <div
          class="col-sm-6 col-md-4 col-xl-3 d-flex justify-content-center pb-3"
        >
          <ImageCard
            label="COVID 19"
            image-src="img/coronavirus-blue.png"
            alt-text="coronavirus"
            :to="{ name: 'catalogue', query: { Categories: 'covid19' } }"
          />
        </div>
        <div
          class="col-sm-6 col-md-4 col-xl-3 d-flex justify-content-center pb-3"
        >
          <ImageCard
            label="Infectious Diseases"
            image-src="img/infected-blue.png"
            alt-text="infected"
            :to="{ name: 'catalogue', query: { Categories: 'infectious' } }"
          />
        </div>
        <div
          class="col-sm-6 col-md-4 col-xl-3 d-flex justify-content-center pb-3"
        >
          <ImageCard
            label="Metabolic Disorders"
            image-src="img/metabolism-blue.png"
            alt-text="metabolism"
            :to="{ name: 'catalogue', query: { Categories: 'metabolic' } }"
          />
        </div>
        <div
          class="col-sm-6 col-md-4 col-xl-3 d-flex justify-content-center pb-3"
        >
          <ImageCard
            label="Nervous System Disorders"
            image-src="img/brain-blue.png"
            alt-text="brain"
            :to="{ name: 'catalogue', query: { Categories: 'nervous_system' } }"
          />
        </div>
        <div
          class="col-sm-6 col-md-4 col-xl-3 d-flex justify-content-center pb-3"
        >
          <ImageCard
            label="Oncology"
            image-src="img/ribbon-blue.png"
            alt-text="Oncology"
            :to="{ name: 'catalogue', query: { Categories: 'oncology' } }"
          />
        </div>
        <div
          class="col-sm-6 col-md-4 col-xl-3 d-flex justify-content-center pb-3"
        >
          <ImageCard
            label="Pediatrics"
            image-src="img/pediatrics-blue.png"
            alt-text="pediatrics"
            :to="{ name: 'catalogue', query: { Categories: 'paediatrics' } }"
          />
        </div>
        <div
          class="col-sm-6 col-md-4 col-xl-3 d-flex justify-content-center pb-3"
        >
          <ImageCard
            label="Population Reference"
            image-src="img/blood-sample-blue.png"
            alt-text="Population Reference"
            :to="{ name: 'catalogue', query: { Categories: 'population' } }"
          />
        </div>
        <div
          class="col-sm-6 col-md-4 col-xl-3 d-flex justify-content-center pb-3"
        >
          <ImageCard
            label="Rare Diseases"
            image-src="img/statistics-blue.png"
            alt-text="Rare Diseases"
            :to="{ name: 'catalogue', query: { Categories: 'rare_disease' } }"
          />
        </div>
        <div
          class="col-sm-12 col-md-8 col-xl-6 cards-section-other d-flex justify-content-center align-items-center"
        >
          <div>
            <p>or proceed to the directory without any selection</p>
            <router-link
              class="btn btn-lg btn-outline-secondary btn-block"
              to="/catalogue"
              >Directory</router-link
            >
          </div>
        </div>
      </div>
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

<script setup lang="ts">
import LandingpageBiobankSpotlight from "../components/landingpage-components/LandingpageBiobankSpotlight.vue";
import LandingpageCallToAction from "../components/landingpage-components/LandingpageCallToAction.vue";
import LandingpageCollectionSpotlight from "../components/landingpage-components/LandingpageCollectionSpotlight.vue";
import { useSettingsStore } from "../stores/settingsStore";
import ImageCard from "../components/ImageCard.vue";
import Button from "../components/Button.vue";
import { computed } from "vue";
import router from "../router";

const settingsStore = useSettingsStore();

withDefaults(
  defineProps<{
    editable: boolean;
  }>(),
  {
    editable: false,
  }
);

interface ILandingpageConfig {
  headerText?: string;
  page_biobank_spotlight?: {
    header?: string;
    biobankName?: string;
    biobankId?: string;
    bodyHtml?: string;
    buttonText?: string;
  };
  page_collection_spotlight?: any;
  page_call_to_actions?: any;
  css?: any;
}

const landingpage = computed(() =>
  settingsStore.configurationFetched
    ? (settingsStore.config.landingpage as ILandingpageConfig)
    : (settingsStore.config as ILandingpageConfig)
);

const biobankSpotlight = computed(
  () => landingpage.value.page_biobank_spotlight || {}
);
const collectionSpotlight = computed(
  () => landingpage.value.page_collection_spotlight
);
const callToActions = computed(() => landingpage.value.page_call_to_actions);
const landingpageCss = computed(() => landingpage.value.css);

const headerText = computed(
  () =>
    landingpage.value.headerText ??
    "Search the BBMRI-ERIC Directory by category"
);
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

.cards-section-other {
  color: #003675;
}
</style>
