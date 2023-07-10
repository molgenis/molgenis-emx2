<template>
  <div class="container mg-network-report-card">
    <Spinner v-if="!loaded" />
    <div v-else class="container-fluid">
      <div class="row">
        <div class="col">
          <!-- Back to previous page buttons -->
          <button class="btn btn-link" @click="back">
            <i class="fa fa-angle-left mr-1" aria-hidden="true" />
            <span>{{ uiText["back"] }}</span>
          </button>
        </div>
      </div>

      <div class="row" v-if="network && loaded">
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
                <Tabs>
                  <Tab :title="`Collections (${collections.length})`">
                    <div
                      v-if="
                        !collections ||
                        !biobanks ||
                        collectionsAvailable ||
                        biobanksAvailable
                      "
                    >
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
                    </div>
                  </Tab>
                  <Tab :title="`Biobanks (${biobanks.length})`">
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
                  </Tab>
                </Tabs>
              </div>
              <!-- Right side card -->
              <div class="col-md-4">
                <div class="card">
                  <div class="card-body">
                    <div class="card-text">
                      <h5>Contact Information</h5>
                      <ReportDetailsList :reportDetails="contact" />
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

<script setup>
import { Spinner, Tab, Tabs } from "molgenis-components";
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import ViewGenerator from "../components/generators/ViewGenerator.vue";
import CollectionTitle from "../components/report-components/CollectionTitle.vue";
import ReportDescription from "../components/report-components/ReportDescription.vue";
import ReportDetailsList from "../components/report-components/ReportDetailsList.vue";
import ReportTitle from "../components/report-components/ReportTitle.vue";
import {
  getCollectionDetails,
  mapContactInfo,
} from "../functions/viewmodelMapper";
import { useNetworkStore } from "../stores/networkStore";
import { useQualitiesStore } from "../stores/qualitiesStore";
import { useSettingsStore } from "../stores/settingsStore";

const router = useRouter();
const route = useRoute();

let loaded = ref(false);

const settingsStore = useSettingsStore();
const networkStore = useNetworkStore();
const qualitiesStore = useQualitiesStore();

const networkPromise = networkStore.loadNetworkReport(route.params.id);
const qualitiesPromise = qualitiesStore.getQualityStandardInformation();
Promise.all([qualitiesPromise, networkPromise]).then(
  () => (loaded.value = true)
);

const uiText = computed(() => settingsStore.uiText);
const networkReport = computed(() => networkStore.networkReport);
const collections = computed(filterCollections);
const collectionsAvailable = computed(() => collections.value?.length);
const biobanks = computed(() => networkReport.value.biobanks);
const biobanksAvailable = computed(() => biobanks.value?.length);
const network = computed(() => networkReport.value.network);
const contact = computed(() => mapContactInfo(network.value));

function back() {
  router.go(-1);
}

function filterCollections() {
  return (
    networkReport.value.collections
      ?.filter((collection) => {
        return !collection.parentCollection;
      })
      .map((col) => getCollectionDetails(col)) || []
  );
}
</script>
