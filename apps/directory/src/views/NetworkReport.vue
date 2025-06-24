<template>
  <div class="container mg-network-report-card">
    <div
      v-if="!loaded"
      class="d-flex justify-content-center align-items-center spinner-container"
    >
      <Spinner />
    </div>
    <div v-else-if="network" class="container-fluid">
      <div class="row">
        <div class="col my-3 shadow-sm d-flex p-2 align-items-center bg-white">
          <Breadcrumb
            class="directory-nav"
            :crumbs="{
              [uiText['home']]: '../#/',
              [network.name]: '/',
            }"
            useRouterLink
          />
        </div>
      </div>

      <div class="row" v-if="network">
        <div class="col">
          <ReportTitle type="Network" :name="network.name" />
          <div class="container">
            <div class="row">
              <div class="col-md-8">
                <p><b>Id: </b>{{ network.id }}</p>
                <ReportDescription
                  :description="network.description"
                  :maxLength="500"
                />
                <div
                  v-if="network.common_network_elements?.length"
                  class="my-5"
                >
                  <h3>Network details</h3>
                  <ul>
                    <li
                      v-for="element of network.common_network_elements"
                      :key="`key-${element}`"
                    >
                      {{ element.label || element.definition }}
                    </li>
                  </ul>
                </div>
                <Tabs :tabIds="['collections', 'biobanks']">
                  <template #collections-header>
                    <CollectionsHeader
                      :collectionCount="collections.length"
                      :subcollectionCount="subcollectionCount"
                    />
                  </template>
                  <template #collections-body>
                    <div class="pt-3">
                      <div
                        v-for="(collection, index) in collections"
                        :key="collection.id"
                      >
                        <hr v-if="index" />
                        <CollectionTitle
                          :title="collection.name"
                          :id="collection.id"
                        />
                        <ViewGenerator :viewmodel="collection.viewmodel" />
                      </div>
                    </div>
                  </template>

                  <template #biobanks-header>
                    Biobanks ({{ biobanks?.length || 0 }})
                  </template>
                  <template #biobanks-body>
                    <div class="pt-3">
                      <div
                        v-for="(biobank, index) in biobanks"
                        :key="biobank.id"
                      >
                        <hr v-if="index" />
                        <h4>
                          <router-link :to="`/biobank/${biobank.id}`">
                            {{ biobank.name }}
                          </router-link>
                        </h4>
                        <ReportDescription
                          :description="biobank.description"
                          :maxLength="250"
                        />
                      </div>
                    </div>
                  </template>
                </Tabs>
              </div>
              <!-- Right side card -->
              <div class="col-md-4">
                <div class="card">
                  <div class="card-body">
                    <div class="card-text">
                      <h5>Contact Information</h5>
                      <ContactInformation
                        :contactInformation="network.contact"
                      />
                      <template v-if="alsoKnownIn.length > 0">
                        <h5>Also Known In</h5>
                        <ReportDetailsList :reportDetails="alsoKnownIn" />
                      </template>
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
//@ts-ignore
import { Breadcrumb, Spinner, Tabs } from "molgenis-components";
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import ViewGenerator from "../components/generators/ViewGenerator.vue";
import CollectionsHeader from "../components/report-components/CollectionsHeader.vue";
import CollectionTitle from "../components/report-components/CollectionTitle.vue";
import ContactInformation from "../components/report-components/ContactInformation.vue";
import ReportDescription from "../components/report-components/ReportDescription.vue";
import ReportDetailsList from "../components/report-components/ReportDetailsList.vue";
import ReportTitle from "../components/report-components/ReportTitle.vue";
import {
  getCollectionDetails,
  mapAlsoKnownIn,
} from "../functions/viewmodelMapper";
import { useNetworkStore } from "../stores/networkStore";
import { useQualitiesStore } from "../stores/qualitiesStore";
import { useSettingsStore } from "../stores/settingsStore";

const route = useRoute();

let loaded = ref(false);

const settingsStore = useSettingsStore();
const networkStore = useNetworkStore();
const qualitiesStore = useQualitiesStore();

const networkPromise = networkStore.loadNetworkReport(
  route.params.id as string
);
const qualitiesPromise = qualitiesStore.getQualityStandardInformation();
Promise.all([qualitiesPromise, networkPromise]).then(
  () => (loaded.value = true)
);

const uiText = computed(() => settingsStore.uiText);
const networkReport = computed(() => networkStore.networkReport);
const collections = computed(() =>
  filterCollections(networkReport.value.collections)
);
const biobanks = computed(() =>
  networkReport.value.biobanks.filter((biobank: Record<string, any>) => {
    return !biobank.withdrawn;
  })
);
const network = computed(() => networkReport.value.network);
const alsoKnownIn = computed(() => mapAlsoKnownIn(network.value));
const subcollectionCount = computed<number>(
  () =>
    networkReport.value.collections
      ?.filter(
        (collection: Record<string, any>) => collection.parent_collection
      )
      .filter((collection: Record<string, any>) => !collection.withdrawn)
      .length || 0
);

function filterCollections(collections: Record<string, any>[]) {
  return (
    collections
      ?.filter((collection: Record<string, any>) => {
        return !collection.parent_collection;
      })
      .filter((collection: Record<string, any>) => {
        return !collection.withdrawn;
      })
      .map((collection: Record<string, any>) =>
        getCollectionDetails(collection)
      ) || []
  );
}
</script>
