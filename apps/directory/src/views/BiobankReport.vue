<template>
  <div class="mg-biobank-card container pb-4">
    <div v-if="biobank.withdrawn" class="alert alert-warning" role="alert">
      {{ uiText["biobank_withdrawn"] }}
    </div>
    <!-- <script
      v-if="bioschemasJsonld && !isLoading"
      v-text="bioschemasJsonld"
      type="application/ld+json"/> -->
    <div v-if="isLoading" class="d-flex justify-content-center mt-5">
      <spinner />
    </div>
    <div v-else class="container-fluid">
      <div class="row">
        <div class="col my-3 shadow-sm d-flex p-2 align-items-center">
          <nav class="directory-nav" aria-label="breadcrumb">
            <ol class="breadcrumb my-1">
              <li class="breadcrumb-item">
                <router-link to="/catalogue" title="Back to the catalogue">
                  {{ uiText["home"] }}
                </router-link>
              </li>
              <li class="breadcrumb-item active text-dark" aria-current="page">
                {{ biobank.name }}
              </li>
            </ol>
          </nav>
          <!-- <check-out
            class="ml-auto"
            :disabled="biobank.withdrawn"
            :bookmark="false"
          /> -->
        </div>
      </div>
      <div class="row" v-if="!this.isLoading">
        <div class="col">
          <report-title type="Biobank" :name="biobank.name"></report-title>
          <div class="container">
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
                    <!-- <collection-selector
                        :disabled="biobank.withdrawn"
                        class="pl-4 ml-auto"
                        :collectionData="collection"
                      /> -->
                  </div>
                  <view-generator
                    class="collection-view"
                    :viewmodel="collection.viewmodel"
                  />
                </div>
              </div>
              <!-- Right side card -->
              <div class="col-md-4">
                <div class="card">
                  <div class="card-body">
                    <div class="card-text">
                      <h5>Contact Information</h5>
                      <report-details-list
                        :reportDetails="contact"
                      ></report-details-list>
                      <h5 v-if="networks && networks.length > 0">Networks</h5>
                      <report-details-list
                        :reportDetails="network"
                        v-for="network in networks"
                        :key="network.id"
                      ></report-details-list>
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

<script>
import { ref } from "vue";
import { useRoute } from "vue-router";
import { useBiobanksStore } from "../stores/biobanksStore";
import { useSettingsStore } from "../stores/settingsStore";
import { Spinner } from "../../../molgenis-components";
import ReportTitle from "../components/report-components/ReportTitle.vue";
import CollectionTitle from "../components/report-components/CollectionTitle.vue";
import ReportDetailsList from "../components/report-components/ReportDetailsList.vue";
import ViewGenerator from "../components/generators/ViewGenerator.vue";
import {
  getBiobankDetails,
  getCollectionDetails,
  mapContactInfo,
  mapNetworkInfo,
  mapObjArray,
} from "../functions/viewmodelMapper";

export default {
  name: "biobank-report-card",
  components: {
    Spinner,
    ReportTitle,
    CollectionTitle,
    ViewGenerator,
    ReportDetailsList,
  },
  setup() {
    const settingsStore = useSettingsStore();
    const biobanksStore = useBiobanksStore();

    const biobankColumns = settingsStore.config.biobankColumns;

    const biobank = ref({});
    const route = useRoute();

    biobanksStore.getBiobankCard(route.params.id).then((result) => {
      biobank.value = result.Biobanks.length
        ? getBiobankDetails(result.Biobanks[0])
        : {};
    });

    return { settingsStore, biobanksStore, biobank, biobankColumns };
  },
  methods: {},
  computed: {
    uiText() {
      return this.settingsStore.uiText;
    },
    isLoading() {
      return this.biobanksStore.waiting;
    },
    biobankDataAvailable() {
      return Object.keys(this.biobank).length;
    },
    collectionsData() {
      return !this.isLoading &&
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
    contact() {
      return this.biobankDataAvailable && this.biobank.contact
        ? mapContactInfo(this.biobank)
        : {};
    },
    quality() {
      return {
        Certification: {
          value: mapObjArray(this.biobank.quality),
          type: "list",
        },
      };
    },
  },
};
</script>
