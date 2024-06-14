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
          <ReportTitle type="Biobank" :name="biobank.name"></ReportTitle>

          <div class="container pl-0">
            <div class="row">
              <div class="col-md-8" v-if="biobankDataAvailable">
                <ViewGenerator :viewmodel="biobank.viewmodel" />

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
                </Tabs>
              </div>
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
import { mapBiobankToBioschemas } from "../functions/bioschemasMapper";
import {
  getBiobankDetails,
  getName,
  mapAlsoKnownIn,
  mapNetworkInfo,
  mapQualityStandards,
  filterCollections,
  filterSubcollections,
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
  },
  setup() {
    const settingsStore = useSettingsStore();
    const biobanksStore = useBiobanksStore();
    const qualitiesStore = useQualitiesStore();
    const biobank = ref<Record<string, any>>({});
    const route = useRoute();

    const collections = computed(() => filterCollections(biobank.value));
    const subcollections = computed(() => filterSubcollections(biobank.value));
    const collectionsAvailable = computed(() => collections.value.length);
    const subcollectionsAvailable = computed(() => subcollections.value.length);

    biobanksStore
      .getBiobank(route.params.id)
      .then((result: Record<string, any>) => {
        biobank.value = result.Biobanks.length
          ? getBiobankDetails(result.Biobanks[0])
          : {};
      });

    return {
      settingsStore,
      biobanksStore,
      qualitiesStore,
      biobank,
      collections,
      collectionsAvailable,
      subcollections,
      subcollectionsAvailable,
    };
  },
  methods: {
    wrapBioschema(schemaData: Record<string, any>) {
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
