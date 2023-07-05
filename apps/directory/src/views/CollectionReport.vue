<template>
  <div class="container mg-collection-report-card pb-4">
    <div
      v-if="collectionDataAvailable && collection.biobank.withdrawn"
      class="alert alert-warning"
      role="alert"
    >
      {{ uiText["collection_withdrawn"] }}
    </div>

    <div
      class="hello"
      v-if="collectionDataAvailable && bioschemasJsonld"
      v-html="bioschemasJsonld"
    ></div>
    <div
      v-if="!collectionDataAvailable"
      class="d-flex justify-content-center align-items-center spinner-container"
    >
      <spinner />
    </div>
    <div class="container-fluid" v-if="collectionDataAvailable">
      <div class="row">
        <div class="col my-3 shadow-sm d-flex p-2 align-items-center bg-white">
          <breadcrumb>
            <ol class="breadcrumb my-1">
              <li class="breadcrumb-item">
                <router-link to="/catalogue" title="Back to the catalogue">
                  {{ uiText["home"] }}
                </router-link>
              </li>
              <li class="breadcrumb-item">
                <router-link
                  :to="'/biobank/' + collection.biobank.id"
                  :title="'Go to biobank ' + collection.biobank.name"
                >
                  {{ collection.biobank.name }}
                </router-link>
              </li>
              <li class="breadcrumb-item" v-if="info.parentCollection">
                <router-link
                  :to="'/collection/' + info.parentCollection.id"
                  :title="
                    'Go to parent collection ' + info.parentCollection.name
                  "
                >
                  {{ info.parentCollection.name }}
                </router-link>
              </li>
              <li class="breadcrumb-item active text-dark" aria-current="page">
                {{ collection.name }}
              </li>
            </ol>
          </breadcrumb>
          <check-out
            v-if="collection"
            class="ml-auto"
            :bookmark="false"
            :disabled="collection.biobank.withdrawn"
          />
        </div>
      </div>

      <div class="row" v-if="collectionDataAvailable">
        <div class="col p-0">
          <report-title type="Collection" :name="collection.name">
          </report-title>

          <div class="container p-0">
            <div class="row">
              <div class="col-md-8">
                <report-collection-details
                  v-if="collection"
                  :biobank="info.biobank"
                  :collection="collection"
                />
              </div>

              <!-- Right side card -->
              <collection-report-info-card
                :info="info"
              ></collection-report-info-card>
            </div>
            <!-- facts data -->
            <!-- <div
              class="row"
              v-if="factsData && Object.keys(factsData).length > 0">
              <facts-table :attribute="factsData"></facts-table>
            </div> -->
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
import { useCollectionStore } from "../stores/collectionStore";
import { useSettingsStore } from "../stores/settingsStore";
import { Spinner } from "../../../molgenis-components";
import Breadcrumb from "../components/micro-components/BreadcrumbComponent.vue";
import ReportTitle from "../components/report-components/ReportTitle.vue";
import CheckOut from "../components/checkout-components/CheckOut.vue";
import ReportCollectionDetails from "../components/report-components/ReportCollectionDetails.vue";
import CollectionReportInfoCard from "../components/report-components/CollectionReportInfoCard.vue";
import { collectionReportInformation } from "../functions/viewmodelMapper";
import { mapCollectionToBioschemas } from "../functions/bioschemasMapper";
import { useQualitiesStore } from "../stores/qualitiesStore";

export default {
  name: "collection-report-card",
  components: {
    Spinner,
    ReportTitle,
    ReportCollectionDetails,
    CollectionReportInfoCard,
    Breadcrumb,
    CheckOut,
  },
  setup() {
    const settingsStore = useSettingsStore();
    const collectionStore = useCollectionStore();
    const qualitiesStore = useQualitiesStore();

    const collection = ref({});
    const route = useRoute();

    collectionStore.getCollectionReport(route.params.id).then((result) => {
      collection.value = result.Collections.length ? result.Collections[0] : {};
    });

    return { settingsStore, collectionStore, qualitiesStore, collection };
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
    collectionDataAvailable() {
      return Object.keys(this.collection).length;
    },
    info() {
      return this.collectionDataAvailable
        ? collectionReportInformation(this.collection)
        : {};
    },
    bioschemasJsonld() {
      return this.collectionDataAvailable
        ? this.wrapBioschema(mapCollectionToBioschemas(this.collection))
        : undefined;
    },
    // factsData() {
    //   // TODO rework this so that facts are stand-alone, this is a workaround because @ReportCollectionDetails
    //   return { value: this.collection.facts };
    // },
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
