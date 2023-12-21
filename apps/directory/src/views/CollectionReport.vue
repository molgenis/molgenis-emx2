<template>
  <div class="container mg-collection-report-card pb-4">
    <div
      v-if="loaded && collectionDataAvailable && collection.biobank.withdrawn"
      class="alert alert-warning"
      role="alert"
    >
      {{ uiText["collection_withdrawn"] }}
    </div>

    <div
      v-if="loaded && collectionDataAvailable && bioschemasJsonld"
      v-html="bioschemasJsonld"
    />
    <div
      v-if="!collectionDataAvailable || !loaded"
      class="d-flex justify-content-center align-items-center spinner-container"
    >
      <spinner />
    </div>
    <div class="container-fluid" v-if="loaded && collectionDataAvailable">
      <div class="row">
        <div class="col my-3 shadow-sm d-flex p-2 align-items-center bg-white">
          <Breadcrumb
            class="directory-nav"
            :crumbs="{
              [uiText['home']]: '../#/',
              [collection.biobank
                .name]: `../#/biobank/${collection.biobank.id}`,
              [collection.name]: `../#/collection/${collection.id}`,
            }"
          />
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
              <collection-report-info-card :info="info" />
            </div>
            <!-- facts data -->
            <div class="row" v-if="facts && facts.length > 0">
              <facts-table class="w-100 px-3" :attribute="facts"></facts-table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import { Breadcrumb, Spinner } from "../../../molgenis-components";
import CheckOut from "../components/checkout-components/CheckOut.vue";
import CollectionReportInfoCard from "../components/report-components/CollectionReportInfoCard.vue";
import ReportCollectionDetails from "../components/report-components/ReportCollectionDetails.vue";
import ReportTitle from "../components/report-components/ReportTitle.vue";
import { mapCollectionToBioschemas } from "../functions/bioschemasMapper";
import { collectionReportInformation } from "../functions/viewmodelMapper";
import { useCollectionStore } from "../stores/collectionStore";
import { useQualitiesStore } from "../stores/qualitiesStore";
import { useSettingsStore } from "../stores/settingsStore";
import FactsTable from "../components/report-components/FactsTable.vue";

const settingsStore = useSettingsStore();
const collectionStore = useCollectionStore();
const qualitiesStore = useQualitiesStore();

const collection = ref({});
const facts = ref({});
const route = useRoute();

let loaded = ref(false);

const collectionsPromise = collectionStore
  .getCollectionReport(route.params.id)
  .then((result) => {
    collection.value = result.Collections.length ? result.Collections[0] : {};
    facts.value =
      result.CollectionFacts && result.CollectionFacts.length
        ? result.CollectionFacts
        : {};
  });
const qualitiesPromise = qualitiesStore.getQualityStandardInformation();
Promise.all([qualitiesPromise, collectionsPromise]).then(() => {
  loaded.value = true;
});

const uiText = computed(() => {
  return settingsStore.uiText;
});
const collectionDataAvailable = computed(() => {
  return Object.keys(collection).length;
});
const info = computed(() => {
  return collectionDataAvailable.value
    ? collectionReportInformation(collection.value)
    : {};
});
const bioschemasJsonld = computed(() => {
  if (collection.value.biobank && collectionDataAvailable.value) {
    return wrapBioschema(mapCollectionToBioschemas(collection.value));
  } else {
    return undefined;
  }
});

function wrapBioschema(schemaData) {
  /** ignore because it is not useless ;) */
  return `<script type="application/ld+json">${JSON.stringify(
    schemaData
  )}<\/script>`;
}
</script>

<style scoped>
.spinner-container {
  height: 70vh;
}
</style>
