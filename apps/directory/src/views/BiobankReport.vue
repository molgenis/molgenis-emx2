<template>
  <div class="mg-biobank-card container pb-4">
    <div
      v-if="biobank.withdrawn"
      class="alert alert-warning ml-n3"
      role="alert"
    >
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
      <Spinner />
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
          <CheckOut
            class="ml-auto"
            :disabled="biobank.withdrawn"
            :bookmark="false"
          />
        </div>
      </div>
      <div class="row" v-if="biobankDataAvailable">
        <div class="col p-0">
          <ReportTitle type="Biobank" :name="biobank.name" />
          <div class="container pl-0">
            <div class="row">
              <div class="col-md-8" v-if="biobankDataAvailable">
                <ViewGenerator
                  v-if="biobank.viewmodel"
                  :viewmodel="biobank.viewmodel"
                />

                <div class="d-flex mt-4 justify-content-between">
                  <small class="w-25">
                    <table class="table table-sm table-borderless">
                      <tbody>
                        <tr>
                          <td class="text-nowrap">
                            Collections: {{ collectionsData.length }}
                          </td>
                        </tr>
                        <tr>
                          <td class="text-nowrap">
                            Subcollections: {{ subcollectionCount }}
                          </td>
                        </tr>
                        <tr>
                          <td class="text-nowrap">
                            Services: {{ biobank.services?.length || 0 }}
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </small>
                </div>

                <Tabs
                  v-if="biobank.collections?.length || biobank.services?.length"
                  :tabs="tabs"
                  @update:active-tab="changeTab"
                  class="mt-1"
                />

                <div v-if="activeTab === 'Collections'">
                  <div
                    v-for="(collection, index) in collectionsData"
                    :key="collection.id"
                  >
                    <hr v-if="index" />
                    <div class="d-flex align-items-center">
                      <CollectionTitle
                        :title="collection.name"
                        :id="collection.id"
                      />
                      <CollectionSelector
                        :disabled="biobank.withdrawn"
                        class="pl-4 ml-auto"
                        :biobankData="biobank"
                        :collectionData="collection"
                      />
                    </div>
                    <CollapseComponent>
                      <ViewGenerator
                        class="collection-view"
                        :viewmodel="collection.viewmodel"
                      />
                    </CollapseComponent>
                  </div>
                </div>

                <ServiceDetails
                  v-else-if="activeTab === 'Services'"
                  v-for="service in biobank.services"
                  class="ml-2 mt-2 mb-4"
                  :service="service"
                  :quality-props="{ label: 'mylabel', value: 'myvalue' }"
                />
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

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
//@ts-ignore
import { Breadcrumb, Spinner } from "molgenis-components";
import Tabs from "../components/Tabs.vue";
import CheckOut from "../components/checkout-components/CheckOut.vue";
import CollectionSelector from "../components/checkout-components/CollectionSelector.vue";
import ViewGenerator from "../components/generators/ViewGenerator.vue";
import ServiceDetails from "../components/partial-views/ServiceDetails.vue";
import CollapseComponent from "../components/report-components/CollapseComponent.vue";
import CollectionTitle from "../components/report-components/CollectionTitle.vue";
import ContactInformation from "../components/report-components/ContactInformation.vue";
import ReportDetailsList from "../components/report-components/ReportDetailsList.vue";
import ReportTitle from "../components/report-components/ReportTitle.vue";
import useErrorHandler from "../composables/errorHandler";
import { mapBiobankToBioschemas } from "../functions/bioschemasMapper";
import {
  getBiobankDetails,
  getCollectionDetails,
  getName,
  mapAlsoKnownIn,
  mapNetworkInfo,
} from "../functions/viewmodelMapper";
import { useBiobanksStore } from "../stores/biobanksStore";
import { useQualitiesStore } from "../stores/qualitiesStore";
import { useSettingsStore } from "../stores/settingsStore";
import { IBiobanks } from "../interfaces/directory";

const settingsStore = useSettingsStore();
const biobanksStore = useBiobanksStore();
const qualitiesStore = useQualitiesStore();

const biobank = ref<IBiobanks>({} as IBiobanks);
const route = useRoute();
const { setError } = useErrorHandler();

qualitiesStore.getQualityStandardInformation();

biobanksStore.getBiobank(route.params.id).then((result: any) => {
  if (result.Biobanks?.length) {
    biobank.value = getBiobankDetails(result.Biobanks[0]);
  } else {
    setError("Biobank not found");
  }
});

type TBiobankDetailTab = "Collections" | "Services";
const activeTab = ref<TBiobankDetailTab>("Collections");

const tabs = computed(() => {
  return {
    Collections: {
      id: "Collections",
      label: `Collections`,
      active: activeTab.value === "Collections",
      disabled: biobank.value?.collections?.length === 0,
    },
    Services: {
      id: "Services",
      label: "Services",
      active: activeTab.value === "Services",
      disabled: biobank.value?.services?.length === 0,
    },
  };
});

function changeTab(tab: TBiobankDetailTab) {
  activeTab.value = tab;
}

const uiText = computed(() => {
  return settingsStore.uiText;
});

const biobankDataAvailable = computed(() => {
  return Object.keys(biobank.value).length;
});

const collectionsData = computed(() => {
  return biobankDataAvailable.value && biobank.value.collections?.length
    ? filterAndSortCollectionsData(biobank.value.collections)
    : [];
});

const subcollectionCount = computed<number>(() => {
  return (
    biobank.value?.collections
      ?.filter(
        (collection: Record<string, any>) => collection.parent_collection
      )
      .filter((collection: Record<string, any>) => !collection.withdrawn)
      .length || 0
  );
});

const networks = computed(() => {
  return biobankDataAvailable.value && biobank.value?.network
    ? mapNetworkInfo(biobank.value)
    : [];
});

const head = computed(() => {
  return biobank.value?.head ? getName(biobank.value.head) : null;
});

const contact = computed(() => {
  return biobank.value?.contact || {};
});

const alsoKnownIn = computed(() => {
  return biobankDataAvailable.value && biobank.value?.also_known
    ? mapAlsoKnownIn(biobank.value)
    : [];
});

const bioschemasJsonld = computed(() => {
  return biobankDataAvailable.value
    ? wrapBioschema(mapBiobankToBioschemas(biobank.value))
    : undefined;
});

function wrapBioschema(schemaData: Record<string, any>) {
  /** ignore because it is not useless ;) */
  return `<script type="application/ld+json">${JSON.stringify(
    schemaData
  )}<\/script>`;
}

function filterAndSortCollectionsData(collections: Record<string, any>[]) {
  return collections
    .filter((collection: Record<string, any>) => !collection.parent_collection)
    .filter(
      (collection: Record<string, any>): Boolean =>
        biobank.value.withdrawn || !collection.withdrawn
    )
    .map((collection: Record<string, any>) =>
      getCollectionDetails(collection, biobank.value.withdrawn)
    )
    .sort(
      (collection1: Record<string, any>, collection2: Record<string, any>) =>
        collection1.name.localeCompare(collection2.name, "en", {
          sensitivity: "base",
        })
    );
}
</script>

<style scoped>
.spinner-container {
  height: 70vh;
}
sup {
  vertical-align: super;
  font-size: small;
}

.layout-table {
  border-collapse: unset;
}
</style>
