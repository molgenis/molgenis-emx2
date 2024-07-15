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
            useRouterLink
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
                <div v-if="!collectionsData.length">
                  This biobank does not contain any collections.
                </div>
                <div v-else>
                  Collection(s): {{ collectionsData.length }} /
                  Subcollection(s):
                  {{ subcollectionCount }}
                </div>
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
                        <li v-if="head">
                          <div class="font-weight-bold mr-1">Head/PI:</div>
                          <div>{{ head }}</div>
                        </li>
                        <li>
                          <ContactInformation
                            :contactInformation="contact"
                            :website="biobank.url"
                          />
                        </li>
                        <li v-if="networks?.length">
                          <h5>Networks</h5>
                          <ReportDetailsList
                            :reportDetails="network"
                            v-for="network in networks"
                            :key="network.id"
                          />
                        </li>
                        <li v-if="alsoKnownIn?.length">
                          <h5>Also Known In</h5>
                          <ReportDetailsList :reportDetails="alsoKnownIn" />
                        </li>
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

<script lang="ts">
import { ref } from "vue";
import { useRoute } from "vue-router";
//@ts-ignore
import { Breadcrumb, Spinner } from "../../../molgenis-components";
import CheckOut from "../components/checkout-components/CheckOut.vue";
import CollectionSelector from "../components/checkout-components/CollectionSelector.vue";
import ViewGenerator from "../components/generators/ViewGenerator.vue";
import CollapseComponent from "../components/report-components/CollapseComponent.vue";
import CollectionTitle from "../components/report-components/CollectionTitle.vue";
import ContactInformation from "../components/report-components/ContactInformation.vue";
import ReportDetailsList from "../components/report-components/ReportDetailsList.vue";
import ReportTitle from "../components/report-components/ReportTitle.vue";
import { mapBiobankToBioschemas } from "../functions/bioschemasMapper";
import {
  getBiobankDetails,
  getCollectionDetails,
  getName,
  mapAlsoKnownIn,
  mapNetworkInfo,
  mapQualityStandards,
} from "../functions/viewmodelMapper";
import { useBiobanksStore } from "../stores/biobanksStore";
import { useQualitiesStore } from "../stores/qualitiesStore";
import { useSettingsStore } from "../stores/settingsStore";
import _ from "lodash";

export default {
  name: "biobank-report-card",
  components: {
    Breadcrumb,
    CheckOut,
    CollapseComponent,
    CollectionTitle,
    CollectionSelector,
    ContactInformation,
    ReportDetailsList,
    ReportTitle,
    Spinner,
    ViewGenerator,
  },
  setup() {
    const settingsStore = useSettingsStore();
    const biobanksStore = useBiobanksStore();
    const qualitiesStore = useQualitiesStore();

    const biobank: Record<string, any> = ref({});
    const route = useRoute();

    biobanksStore
      .getBiobank(route.params.id)
      .then((result: Record<string, any>) => {
        biobank.value = result.Biobanks.length
          ? getBiobankDetails(result.Biobanks[0])
          : {};
      });

    return { settingsStore, biobanksStore, qualitiesStore, biobank };
  },
  methods: {
    wrapBioschema(schemaData: Record<string, any>) {
      /** ignore because it is not useless ;) */
      return `<script type="application/ld+json">${JSON.stringify(
        schemaData
      )}<\/script>`;
    },
    filterAndSortCollectionsData(collections: Record<string, any>[]) {
      return collections
        .filter(
          (collection: Record<string, any>) => !collection.parent_collection
        )
        .filter(
          (collection: Record<string, any>): Boolean =>
            this.biobank.withdrawn || !collection.withdrawn
        )
        .map((collection: Record<string, any>) =>
          getCollectionDetails(collection, this.biobank.withdrawn)
        )
        .sort(
          (
            collection1: Record<string, any>,
            collection2: Record<string, any>
          ) =>
            collection1.name.localeCompare(collection2.name, "en", {
              sensitivity: "base",
            })
        );
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
      return this.biobankDataAvailable && this.biobank.collections?.length
        ? this.filterAndSortCollectionsData(this.biobank.collections)
        : [];
    },
    subcollectionCount() {
      return this.biobank?.collections?.filter(
        (biobank: Record<string, any>) => biobank.parent_collection
      ).length;
    },
    networks() {
      return this.biobankDataAvailable && this.biobank.network
        ? mapNetworkInfo(this.biobank)
        : [];
    },
    head() {
      return this.biobank?.head ? getName(this.biobank.head) : null;
    },
    contact() {
      return this.biobank?.contact || {};
    },
    alsoKnownIn() {
      return this.biobankDataAvailable && this.biobank.also_known
        ? mapAlsoKnownIn(this.biobank)
        : [];
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
    await this.qualitiesStore.getQualityStandardInformation();
  },
};
</script>

<style scoped>
.spinner-container {
  height: 70vh;
}
</style>
