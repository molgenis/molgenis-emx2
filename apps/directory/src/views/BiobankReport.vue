<template>
  <div class="mg-biobank-card container pb-4">
    <div v-if="biobank.withdrawn" class="alert alert-warning" role="alert">
      {{ uiText["biobank_withdrawn"] }}
    </div>
    <div
      v-if="biobankDataAvailable && bioschemasJsonld"
      v-html="bioschemasJsonld"
    />
    <div
      v-if="!biobankDataAvailable"
      class="d-flex justify-content-center align-items-center spinner-container"
    >
      <spinner />
    </div>
    <div v-else class="container-fluid pl-0">
      <div class="row">
        <div class="col my-3 shadow-sm d-flex p-2 align-items-center bg-white">
          <Breadcrumb
            class="directory-nav"
            :crumbs="{
              [uiText['home']]: '../#/',
              [biobank.name]: '/',
            }"
          />
          <check-out
            class="ml-auto"
            :disabled="biobank.withdrawn"
            :bookmark="false"
          />
        </div>
      </div>
      <div class="row" v-if="biobankDataAvailable">
        <div class="col p-0">
          <report-title type="Biobank" :name="biobank.name"></report-title>
          <div class="container pl-0">
            <div class="row">
              <div class="col-md-8" v-if="biobankDataAvailable">
                <view-generator :viewmodel="biobank.viewmodel" />

                <!-- Collection Part -->
                <h3 class="mt-4">Collections</h3>
                <div
                  v-for="(collection, index) in collectionsData"
                  :key="collection.id"
                >
                  <hr v-if="index" />
                  <div class="d-flex align-items-center">
                    <collection-title
                      :title="collection.name"
                      :id="collection.id"
                    />
                    <collection-selector
                      :disabled="biobank.withdrawn"
                      class="pl-4 ml-auto"
                      :biobankData="biobank"
                      :collectionData="collection"
                    />
                  </div>
                  <collapse-component>
                    <view-generator
                      class="collection-view"
                      :viewmodel="collection.viewmodel"
                    />
                  </collapse-component>
                </div>
              </div>
              <!-- Right side card -->
              <div class="col-md-4">
                <div class="card">
                  <div class="card-body">
                    <div class="card-text">
                      <h5>Contact Information</h5>
                      <ul class="right-content-list">

                        <li><span class="font-weight-bold mr-1">Head/PI:</span></li>
                        <report-details-list
                          :reportDetails="head"
                        ></report-details-list>
                        <span class="font-weight-bold mr-1">Contact:</span>
                        <report-details-list
                          :reportDetails="contact"
                        ></report-details-list>
                        <h5 v-if="networks && networks.length > 0">Networks</h5>
                        <report-details-list
                          :reportDetails="network"
                          v-for="network in networks"
                          :key="network.id"
                        ></report-details-list>
                        <template v-if="alsoKnownIn.length > 0">
                          <h5>Also Known In</h5>
                          <ReportDetailsList :reportDetails="alsoKnownIn" />
                        </template>
                        <h5
                          v-if="
                            quality &&
                            quality.Certification &&
                            quality.Certification.value.length > 0
                          "
                        >
                          Quality
                        </h5>
                        <report-details-list
                          :reportDetails="quality"
                        ></report-details-list>
                      </ul>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<!-- eslint-disable no-useless-escape -->
<script>
import { ref } from "vue";
import { useRoute } from "vue-router";
import { Breadcrumb, Spinner } from "../../../molgenis-components";
import CheckOut from "../components/checkout-components/CheckOut.vue";
import CollectionSelector from "../components/checkout-components/CollectionSelector.vue";
import ViewGenerator from "../components/generators/ViewGenerator.vue";
import CollapseComponent from "../components/report-components/CollapseComponent.vue";
import CollectionTitle from "../components/report-components/CollectionTitle.vue";
import ReportDetailsList from "../components/report-components/ReportDetailsList.vue";
import ReportTitle from "../components/report-components/ReportTitle.vue";
import { mapBiobankToBioschemas } from "../functions/bioschemasMapper";
import {
  getBiobankDetails,
  getCollectionDetails,
  mapHeadInfo,
  mapContactInfo,
  mapNetworkInfo,
  mapQualityStandards,
  mapAlsoKnownIn,
} from "../functions/viewmodelMapper";
import { useBiobanksStore } from "../stores/biobanksStore";
import { useQualitiesStore } from "../stores/qualitiesStore";
import { useSettingsStore } from "../stores/settingsStore";

export default {
  name: "biobank-report-card",
  components: {
    Spinner,
    ReportTitle,
    CollectionTitle,
    ViewGenerator,
    ReportDetailsList,
    CollapseComponent,
    CheckOut,
    CollectionSelector,
    Breadcrumb,
  },
  setup() {
    const settingsStore = useSettingsStore();
    const biobanksStore = useBiobanksStore();
    const qualitiesStore = useQualitiesStore();

    const biobank = ref({});
    const route = useRoute();

    biobanksStore.getBiobank(route.params.id).then((result) => {
      biobank.value = result.Biobanks.length
        ? getBiobankDetails(result.Biobanks[0])
        : {};
    });

    return { settingsStore, biobanksStore, qualitiesStore, biobank };
  },
  methods: {
    wrapBioschema(schemaData) {
      /** ignore because it is not useless ;) */
      return `<script type="application/ld+json">${JSON.stringify(
        schemaData
      )}<\/script>`;
    },
  },
  computed: {
    uiText() {
      return this.settingsStore.uiText;
    },
    biobankDataAvailable() {
      return Object.keys(this.biobank).length;
    },
    collectionsData() {
      return this.biobankDataAvailable &&
        this.biobank.collections &&
        this.biobank.collections.length
        ? this.biobank.collections
            .filter((it) => !it.parent_collection)
            .map((col) => getCollectionDetails(col))
        : [];
    },
    networks() {
      return this.biobankDataAvailable && this.biobank.network
        ? mapNetworkInfo(this.biobank)
        : [];
    },
    head() {
      return this.biobankDataAvailable && this.biobank.head
        ? mapHeadInfo(this.biobank)
        : {};
    },
    contact() {
      return this.biobankDataAvailable && this.biobank.contact
        ? mapContactInfo(this.biobank)
        : {};
    },
    alsoKnownIn() {
      return this.biobankDataAvailable && this.biobank.also_known
        ? mapAlsoKnownIn(this.biobank)
        : {};
    },
    quality() {
      return {
        Certification: {
          value: mapQualityStandards(this.biobank.quality),
          type: "list",
        },
      };
    },
    bioschemasJsonld() {
      return this.biobankDataAvailable
        ? this.wrapBioschema(mapBiobankToBioschemas(this.biobank))
        : undefined;
    },
  },
  async mounted() {
    this.showCollections = this.settingsStore.config.biobankCardShowCollections;
    await this.qualitiesStore.getQualityStandardInformation();
  },
};
</script>

<style scoped>
.spinner-container {
  height: 70vh;
}
</style>
