<template>
  <div class="container mg-collection-report-card pb-4">
    <div
      v-if="!loaded"
      class="d-flex justify-content-center align-items-center spinner-container"
    >
      <spinner />
    </div>

    <div
      v-if="loaded && collection?.biobank?.withdrawn"
      class="alert alert-warning"
      role="alert"
    >
      {{ uiText["collection_withdrawn"] }}
    </div>

    <div
      v-if="loaded && collection && bioschemasJsonld"
      v-html="bioschemasJsonld"
    />

    <div class="container-fluid" v-if="loaded && collection">
      <div class="row">
        <div class="col my-3 shadow-sm d-flex p-2 align-items-center bg-white">
          <Breadcrumb
            class="directory-nav"
            :crumbs="{
              [uiText['home']]: '../#/',
              [collection?.biobank
                ?.name]: `../biobank/${collection?.biobank?.id}`,
              [collection?.name]: `../#/collection/${collection?.id}`,
            }"
            useRouterLink
          />
          <check-out
            v-if="collection"
            class="ml-auto"
            :bookmark="false"
            :disabled="collection?.biobank?.withdrawn"
          />
        </div>
      </div>

      <div class="row">
        <div class="col p-0">
          <ReportTitle type="Collection" :name="collection?.name" />

          <div class="container p-0">
            <div class="row">
              <div class="col-md-8">
                <ReportCollectionDetails
                  v-if="collection"
                  :biobank="info.biobank"
                  :collection="collection"
                />
              </div>
              <!-- Right side card -->
              <CollectionReportInfoCard :info="info" :collection="collection" />
            </div>
            <!-- facts data -->
            <div class="row" v-if="facts && facts.length > 0">
              <FactsTable class="w-100 px-3" :attribute="facts" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { Breadcrumb, Spinner } from "../../../molgenis-components";
import CheckOut from "../components/checkout-components/CheckOut.vue";
import CollectionReportInfoCard from "../components/report-components/CollectionReportInfoCard.vue";
import FactsTable from "../components/report-components/FactsTable.vue";
import ReportCollectionDetails from "../components/report-components/ReportCollectionDetails.vue";
import ReportTitle from "../components/report-components/ReportTitle.vue";
import useErrorHandler from "../composables/errorHandler";
import { mapCollectionToBioschemas } from "../functions/bioschemasMapper";
import { collectionReportInformation } from "../functions/viewmodelMapper";
import { useCollectionStore } from "../stores/collectionStore";
import { useQualitiesStore } from "../stores/qualitiesStore";
import { useSettingsStore } from "../stores/settingsStore";

const settingsStore = useSettingsStore();
const collectionStore = useCollectionStore();
const qualitiesStore = useQualitiesStore();

const collection = ref();
const facts = ref({});
const route = useRoute();
const { setError } = useErrorHandler();

let loaded = ref(false);

loadCollectionReport(route.params.id);

watch(route, async (route) => {
  loadCollectionReport(route.params.id);
});

const uiText = computed(() => {
  return settingsStore.uiText;
});

const info = computed(() => {
  return collection.value ? collectionReportInformation(collection.value) : {};
});

const bioschemasJsonld = computed(() => {
  if (collection.value?.biobank) {
    return wrapBioschema(mapCollectionToBioschemas(collection.value));
  } else {
    return undefined;
  }
});

function loadCollectionReport(id) {
  loaded.value = false;
  const collectionsPromise = collectionStore
    .getCollectionReport(id)
    .then((result) => {
      if (result.Collections?.length) {
        collection.value = result.Collections[0];
      } else {
        setError("Collection not found");
      }
      facts.value =
        result.CollectionFacts && result.CollectionFacts.length
          ? result.CollectionFacts
          : {};
    });
  const qualitiesPromise = qualitiesStore.getQualityStandardInformation();
  Promise.all([qualitiesPromise, collectionsPromise]).then(() => {
    loaded.value = true;
  });
}

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
