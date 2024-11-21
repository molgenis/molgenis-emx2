<template>
  <article
    :class="[
      {
        'border border-secondary': biobankInSelection,
        'back-side': showCollections,
      },
      'biobank-card',
    ]"
  >
    <section class="d-flex flex-column align-items-center">
      <div class="d-flex flex-column h-100 align-self-stretch">
        <HeaderSection
          :biobank="biobank"
          :hasBiobankQuality="hasBiobankQuality"
          :qualityInfo="qualityInfo"
        />

        <template v-if="!showCollections">
          <div class="mb-1 shadow-sm" v-if="numberOfCollections">
            <button
              class="btn btn-link text-info pl-2"
              @click.prevent="showCollections = true"
            >
              Collections
            </button>
          </div>
          <div class="p-2 pt-1 biobank-section flex-grow-1">
            <small>
              <ViewGenerator :viewmodel="biobankcardViewmodel" />
              <MatchesOn :viewmodel="biobank" />
              <router-link
                :to="'/biobank/' + biobank.id"
                :title="`${biobank.name} details`"
                class="text-info ml-1"
              >
                <span>More details</span>
              </router-link>
            </small>
          </div>
        </template>

        <template v-else>
          <div class="d-flex mb-1 shadow-sm">
            <button
              class="btn btn-link text-info pl-2"
              @click.prevent="showCollections = false"
            >
              {{ uiText["card_biobank_details"] }}
            </button>
          </div>
          <div class="collections-section flex-grow-1">
            <div class="pl-2 pt-2 d-flex" v-if="numberOfCollections">
              <h5>
                {{
                  `${numberOfCollections} collection${
                    numberOfCollections === 1 ? "" : "s"
                  } ${hasActiveFilters ? "found" : "available"}`
                }}
              </h5>
              <collection-selector
                v-if="numberOfCollections > 1"
                class="text-right mr-1 ml-auto align-self-center"
                :biobankData="biobank"
                :collectionData="biobank.collections"
                bookmark
                iconOnly
                multi
              />
            </div>
            <hr class="mt-1" v-if="numberOfCollections" />
            <div v-else class="pl-2">
              {{
                hasActiveFilters
                  ? "No collections found with currently active filters"
                  : "This biobank has no collections yet."
              }}
            </div>
            <div
              class="collection-items mx-1"
              v-for="(collectionDetail, index) of biobank.collectionDetails"
              :key="collectionDetail.id"
            >
              <div v-if="showCollections" class="mb-2">
                <div class="pl-2 pt-2 d-flex">
                  <router-link
                    :to="'/collection/' + collectionDetail.id"
                    title="Collection details"
                    class="text-dark"
                  >
                    <span
                      class="fa fa-server collection-icon fa-lg mr-2 text-primary"
                      aria-hidden="true"
                    />
                    <span class="collection-name">
                      {{ collectionDetail.name }}
                    </span>
                  </router-link>
                  <div class="ml-auto">
                    <collection-selector
                      class="ml-auto"
                      :biobankData="biobank"
                      :collectionData="collectionDetail"
                      iconOnly
                      bookmark
                    />
                  </div>
                </div>

                <small>
                  <ViewGenerator
                    class="p-1"
                    :viewmodel="collectionViewmodel(collectionDetail)"
                  />

                  <MatchesOn :viewmodel="collectionDetail" class="px-1 ml-1" />
                  <router-link
                    :to="'/collection/' + collectionDetail.id"
                    :title="`${collectionDetail.name} details`"
                    class="text-info ml-1 pl-1"
                  >
                    <span>More details</span>
                  </router-link>
                </small>
                <hr v-if="index != lastCollection" />
                <div v-else class="pb-3"></div>
              </div>
            </div>
          </div>
        </template>

        <div v-if="biobank.services?.length">
          {{ biobank.services?.length }} services available
          <div v-for="service in biobank.services">
            <div class="pl-2 pt-2 d-flex">
              <router-link
                :to="'/services/' + service.id"
                title="Service details"
                class="text-dark"
              >
                <span
                  class="fa fa-server fa-lg mr-2 text-primary"
                  aria-hidden="true"
                />
                <span class="collection-name">
                  {{ service.name }}
                </span>
              </router-link>
              <div class="ml-auto">
                <!-- <collection-selector
                      class="ml-auto"
                      :biobankData="biobank"
                      :collectionData="collectionDetail"
                      iconOnly
                      bookmark
                    /> -->
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </article>
</template>

<script setup lang="ts">
import { getBiobankDetails } from "../../functions/viewmodelMapper";
import ViewGenerator from "../generators/ViewGenerator.vue";
import CollectionSelector from "../checkout-components/CollectionSelector.vue";
import MatchesOn from "../biobankcards-components/MatchesOn.vue";
import { useSettingsStore } from "../../stores/settingsStore";
import { useQualitiesStore } from "../../stores/qualitiesStore";
import { useCheckoutStore } from "../../stores/checkoutStore";
import { useFiltersStore } from "../../stores/filtersStore";
import HeaderSection from "./biobackCardSections/HeaderSection.vue";
import { computed, onBeforeMount, ref } from "vue";

const settingsStore = useSettingsStore();
const qualitiesStore = useQualitiesStore();
const checkoutStore = useCheckoutStore();
const filtersStore = useFiltersStore();

const props = withDefaults(
  defineProps<{
    fullSize: boolean;
    biobank: {
      services?: any;
      collections: any;
      label?: string;
      collectionDetails: any;
      id: string;
      name: string;
    };
  }>(),
  {
    fullSize: false,
  }
);

const biobankSelected = ref(false);
const showCollections = ref(false);
const activeTab = ref("Collections");

//async beforeMount
onBeforeMount(async () => {
  await qualitiesStore.getQualityStandardInformation();
  showCollections.value = settingsStore.config.biobankCardShowCollections;
});

function collectionViewmodel(collectiondetails: Record<string, any>) {
  const attributes = [];
  for (const item of settingsStore.config.collectionColumns) {
    if (item.showOnBiobankCard) {
      attributes.push(
        collectiondetails.viewmodel.attributes.find(
          (vm: Record<string, any>) => vm.label === item.label
        )
      );
    }
  }
  return { attributes };
}

function getQualityInfo(key: string) {
  return qualityStandardsDictionary[key];
}

const qualityInfo = computed(() => {
  return biobankQualities.value.map((quality: Record<string, any>) => {
    return getQualityInfo(quality.quality_standard.name);
  });
});

const hasActiveFilters = computed(() => filtersStore.hasActiveFilters);

const uiText = computed(() => settingsStore.uiText);

const lastCollection = computed(
  () => props.biobank.collectionDetails.length - 1
);

const numberOfCollections = computed(() =>
  props.biobank.collections ? props.biobank.collections.length : 0
);

const biobankcardViewmodel = computed(() => {
  const { viewmodel } = getBiobankDetails(props.biobank);
  const attributes = [];

  for (const item of settingsStore.config.biobankColumns) {
    if (item.showOnBiobankCard) {
      attributes.push(
        viewmodel.attributes.find(
          (vm: Record<string, any>) => vm.label === item.label
        )
      );
    }
  }
  return { attributes };
});

const hasBiobankQuality = computed(() => {
  return biobankcardViewmodel.value.attributes.some(
    (attr) => attr.type === "quality" && attr.value && attr.value.length
  );
});

const biobankQualities = computed(() => {
  return biobankcardViewmodel.value.attributes.find(
    (attr) => attr.type === "quality"
  ).value;
});

const qualityStandardsDictionary: Record<string, any> = computed(() => {
  return qualitiesStore.qualityStandardsDictionary;
});

const biobankInSelection = computed(() => {
  const biobankIdentifier: string = props.biobank.label || props.biobank.name;
  return (
    //@ts-ignore can be removed once checkoutStore is ts
    checkoutStore.selectedCollections[biobankIdentifier] !== undefined
  );
});
</script>

<style scoped>
.collection-icon {
  position: relative;
  top: 0.25em;
  line-height: unset;

  clip-path: inset(-15% 0% 75% 0%);
}

.certificate-icon {
  font-size: 0.8rem;
}
</style>

<style>
.btn-link:focus {
  box-shadow: none;
}

.biobank-section,
.collections-section {
  overflow: auto;
}

.collection-items {
  word-break: break-word;
}

.collection-items th {
  width: 25%;
}

.biobank-card {
  width: 25rem;
}

.biobank-card > header,
.collection-header {
  display: flex;
  min-height: 3rem;
  flex-direction: column;
  justify-content: center;
}

/* TODO put in theme */
.biobank-card-header {
  background-color: #efefef;
}

article {
  padding: 1.5rem;
}

article footer {
  padding: 1.5rem 0 0 0;
}

article {
  padding: 0;
  position: relative;
  height: 28rem;
}

article {
  box-shadow: 0 6.4px 14.4px 0 rgba(0, 0, 0, 0.132),
    0 1.2px 3.6px 0 rgba(0, 0, 0, 0.108);
}

article section {
  height: 100%;
  width: 100%;
}

.right-content-list {
  list-style-type: none;
  margin-left: -2.5rem;
}

.right-content-list:not(:last-child) {
  margin-bottom: 1.5rem;
}

.right-content-list li {
  margin-bottom: 0.5rem;
}

.popover-content {
  margin-bottom: 15px;
}

.popover-content:last-child {
  margin-bottom: 0;
}

.popover-content .quality-standard-label {
  font-weight: 700;
}

.popover-content .quality-standard-definition {
  font-weight: 600;
}
</style>
