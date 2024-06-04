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

                <Tabs>
                  <template #titleAddon="{ title }">
                    <InfoPopover
                      v-if="title.startsWith('Collections')"
                      faIcon="fa-regular fa-circle-question"
                      textColor="text-info"
                      class="tab-icon ml-2"
                      style=""
                      popover-placement="top"
                    >
                      <div class="popover-content">
                        Collections: {{ collectionsAvailable }}<br />
                        Subcollections: {{ subcollectionsAvailable }}
                      </div>
                    </InfoPopover>
                  </template>
                  <Tab
                    :title="`Collections (${collectionsAvailable} / ${subcollectionsAvailable})`"
                  >
                    <div v-if="collectionsAvailable">
                      <div class="pt-3">
                        <div
                          v-for="(collection, index) in collectionsData"
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
                </Tabs>
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
import { ref, computed } from "vue";
import { useRoute } from "vue-router";
//@ts-ignore
import {
  Breadcrumb,
  Spinner,
  Tabs,
  Tab,
  InfoPopover,
} from "../../../molgenis-components";
import CheckOut from "../components/checkout-components/CheckOut.vue";
import CollectionSelector from "../components/checkout-components/CollectionSelector.vue";
import ViewGenerator from "../components/generators/ViewGenerator.vue";
import CollapseComponent from "../components/report-components/CollapseComponent.vue";
import CollectionTitle from "../components/report-components/CollectionTitle.vue";
import ContactInformation from "../components/report-components/ContactInformation.vue";
import ReportDetailsList from "../components/report-components/ReportDetailsList.vue";
import ReportTitle from "../components/report-components/ReportTitle.vue";
import ReportDescription from "../components/report-components/ReportDescription.vue";
import { mapBiobankToBioschemas } from "../functions/bioschemasMapper";
import {
  getBiobankDetails,
  getCollectionDetails,
  getName,
  mapAlsoKnownIn,
  mapNetworkInfo,
  mapQualityStandards,
  mapSubcollections,
} from "../functions/viewmodelMapper";
import { useBiobanksStore } from "../stores/biobanksStore";
import { useQualitiesStore } from "../stores/qualitiesStore";
import { useSettingsStore } from "../stores/settingsStore";

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
    Tabs,
    Tab,
    InfoPopover,
    ReportDescription,
  },
  setup() {
    const settingsStore = useSettingsStore();
    const biobanksStore = useBiobanksStore();
    const qualitiesStore = useQualitiesStore();
    const route = useRoute();

    const biobank = ref<Record<string, any>>({});
    const collections = computed(filterCollections);
    const subcollections = computed(filterSubcollections);

    biobanksStore
      .getBiobank(route.params.id)
      .then((result: Record<string, any>) => {
        biobank.value = result.Biobanks.length
          ? getBiobankDetails(result.Biobanks[0])
          : {};
      });

    function filterCollections() {
      return (
        biobank.value.collections
          ?.filter(
            (collection: Record<string, any>) => !collection.parent_collection
          )
          .map((collection: Record<string, any>) =>
            getCollectionDetails(collection)
          ) || []
      );
    }

    function filterSubcollections() {
      return (
        biobank.value.collections
          ?.filter(
            (collection: Record<string, any>) =>
              collection.sub_collections && collection.sub_collections.length
          )
          .map((collection: Record<string, any>) =>
            mapSubcollections(collection.sub_collections, 1)
          )
          .flat() || []
      );
    }

    return {
      settingsStore,
      biobanksStore,
      qualitiesStore,
      biobank,
      collections,
      subcollections,
    };
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
    collectionsAvailable() {
      return this.collections.length;
    },
    subcollectionsAvailable() {
      return this.subcollections.length;
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
