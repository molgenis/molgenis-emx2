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
      <div class="align-self-stretch">
        <header class="border-0 biobank-card-header p-1">
          <h5 class="pt-1 pl-1 pr-1 mt-1">
            <router-link
              :to="'/biobank/' + biobank.id"
              title="Biobank details"
              class="text-dark"
            >
              <span
                class="fa fa-server mr-2 text-primary"
                aria-hidden="true"
              ></span>
              <span class="biobank-name">{{ biobank.name }}</span>
              <sup
                v-if="hasBiobankQuality"
                class="d-inline-block"
                aria-hidden="true"
              >
                <info-popover
                  faIcon="fa-regular fa-circle-check"
                  textColor="text-success"
                  class="ml-1 certificate-icon"
                  popover-placement="right"
                >
                  <div
                    class="popover-content"
                    v-for="quality of biobankQualities"
                    :key="quality.label"
                  >
                    <table v-if="getQualityInfo(quality.label)">
                      <tbody>
                        <th class="pr-3">
                          {{ getQualityInfo(quality.label).label }}
                        </th>
                        <td>
                          {{ getQualityInfo(quality.label).definition }}
                        </td>
                      </tbody>
                    </table>
                  </div>
                </info-popover>
              </sup>
            </router-link>
          </h5>
        </header>

        <div v-if="!showCollections">
          <div class="mb-1 shadow-sm" v-if="numberOfCollections">
            <button
              class="btn btn-link text-info pl-2"
              @click.prevent="showCollections = true"
            >
              {{ uiText["card_collections_details"] }}
            </button>
          </div>
          <div class="p-2 pt-1 biobank-section" :style="cardContainerHeight">
            <small>
              <view-generator :viewmodel="biobankcardViewmodel" />
              <matches-on :viewmodel="biobank" />
              <router-link
                :to="'/biobank/' + biobank.id"
                :title="`${biobank.name} details`"
                class="text-info ml-1"
              >
                <span>More details</span>
              </router-link>
            </small>
          </div>
        </div>

        <div v-else>
          <div class="d-flex mb-1 shadow-sm">
            <button
              class="btn btn-link text-info pl-2"
              @click.prevent="showCollections = false"
            >
              {{ uiText["card_biobank_details"] }}
            </button>
          </div>
          <div class="collections-section" :style="cardContainerHeight">
            <div class="pl-2 pt-2 d-flex" v-if="numberOfCollections">
              <h5>
                {{
                  hasActiveFilters
                    ? `${numberOfCollections} collection${
                        numberOfCollections === 1 ? "" : "s"
                      } found`
                    : `${numberOfCollections} collection${
                        numberOfCollections === 1 ? "" : "s"
                      } available`
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
              ></collection-selector>
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
                    ></span>
                    <span class="collection-name">{{
                      collectionDetail.name
                    }}</span>
                  </router-link>
                  <div class="ml-auto">
                    <collection-selector
                      class="ml-auto"
                      :biobankData="biobank"
                      :collectionData="collectionDetail"
                      iconOnly
                      bookmark
                    ></collection-selector>
                  </div>
                </div>

                <small>
                  <view-generator
                    class="p-1"
                    :viewmodel="collectionViewmodel(collectionDetail)"
                  />

                  <matches-on :viewmodel="collectionDetail" class="px-1 ml-1" />
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
        </div>
      </div>
    </section>
  </article>
</template>

<script>
import {
  getBiobankDetails,
  getCollectionDetails,
} from "../../functions/viewmodelMapper";
import ViewGenerator from "../generators/ViewGenerator.vue";
import CollectionSelector from "../checkout-components/CollectionSelector.vue";
import { InfoPopover } from "molgenis-components";
import MatchesOn from "../biobankcards-components/MatchesOn.vue";
import { useSettingsStore } from "../../stores/settingsStore";
import { useQualitiesStore } from "../../stores/qualitiesStore";
import { useCheckoutStore } from "../../stores/checkoutStore";
import { useFiltersStore } from "../../stores/filtersStore";

export default {
  setup() {
    const settingsStore = useSettingsStore();
    const qualitiesStore = useQualitiesStore();
    const checkoutStore = useCheckoutStore();
    const filtersStore = useFiltersStore();
    return { settingsStore, qualitiesStore, checkoutStore, filtersStore };
  },
  components: {
    ViewGenerator,
    CollectionSelector,
    InfoPopover,
    MatchesOn,
  },
  props: {
    fullSize: {
      type: Boolean,
      default: () => false,
    },
    biobank: {
      type: [Object, String],
    },
  },
  data() {
    return {
      biobankSelected: false,
      showCollections: false,
    };
  },
  methods: {
    getCollectionDetails,
    collectionViewmodel(collectiondetails) {
      const attributes = [];
      for (const item of this.settingsStore.config.collectionColumns) {
        if (item.showOnBiobankCard) {
          attributes.push(
            collectiondetails.viewmodel.attributes.find(
              (vm) => vm.label === item.label
            )
          );
        }
      }
      return { attributes };
    },
    getQualityInfo(key) {
      return this.qualityStandardsDictionary[key];
    },
  },
  computed: {
    hasActiveFilters() {
      return this.filtersStore.hasActiveFilters;
    },
    uiText() {
      return this.settingsStore.uiText;
    },
    lastCollection() {
      return this.biobank.collectionDetails.length - 1;
    },
    numberOfCollections() {
      return this.biobank.collections ? this.biobank.collections.length : 0;
    },
    cardContainerHeight() {
      const charactersInName = this.biobank.name.length;

      let height = 20.5; // default

      if (charactersInName <= 30) {
        height = 22.2;
      }

      /** When a biobank name is too long it will take three rows (most of the time), tipping point is 80 characters. */
      if (charactersInName >= 80) {
        height = 19;
      }

      return `height: ${height}rem;max-height: ${height}rem;`;
    },
    biobankcardViewmodel() {
      const { viewmodel } = getBiobankDetails(this.biobank);
      const attributes = [];

      for (const item of this.settingsStore.config.biobankColumns) {
        if (item.showOnBiobankCard) {
          attributes.push(
            viewmodel.attributes.find((vm) => vm.label === item.label)
          );
        }
      }
      return { attributes };
    },
    hasBiobankQuality() {
      return this.biobankcardViewmodel.attributes.some(
        (attr) => attr.type === "quality" && attr.value && attr.value.length
      );
    },
    biobankQualities() {
      return this.biobankcardViewmodel.attributes.find(
        (attr) => attr.type === "quality"
      ).value;
    },
    qualityStandardsDictionary() {
      return this.qualitiesStore.qualityStandardsDictionary;
    },
    biobankInSelection() {
      const biobankIdentifier = this.biobank.label || this.biobank.name;
      return (
        this.checkoutStore.selectedCollections[biobankIdentifier] !== undefined
      );
    },
  },
  async beforeMount() {
    await this.qualitiesStore.getQualityStandardInformation();
  },
  mounted() {
    this.showCollections = this.settingsStore.config.biobankCardShowCollections;
  },
};
</script>

<style scoped>
.collection-icon {
  position: relative;
  top: 0.25em;
  line-height: unset;
  clip-path: inset(0% 0% 50% 0%);
}

.certificate-icon {
  font-size: 0.8rem;
}
</style>

<style scoped>
.collection-icon {
  position: relative;
  top: 0.25em;
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
</style>
