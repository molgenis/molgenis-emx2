<script setup lang="ts">
import { IBiobanks } from "../../interfaces/directory";
import { useSettingsStore } from "../../stores/settingsStore";
import { computed } from "vue";
import CollectionSelector from "../../components/checkout-components/CollectionSelector.vue";
import ViewGenerator from "../generators/ViewGenerator.vue";
import MatchesOn from "./MatchesOn.vue";

const props = withDefaults(
  defineProps<{
    biobank: IBiobanks;
    hasActiveFilters?: boolean;
  }>(),
  {
    hasActiveFilters: false,
  }
);

const numberOfCollections = computed(() =>
  props.biobank.collections ? props.biobank.collections.length : 0
);

function collectionViewmodel(collectiondetails: Record<string, any>) {
  const attributes = [];
  for (const item of useSettingsStore().config.collectionColumns) {
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
</script>

<template>
  <div class="collections-section flex-grow-1">
    <div class="pl-2 pt-2 d-flex" v-if="numberOfCollections">
      <h5 v-if="numberOfCollections > 1">
        {{
          `${numberOfCollections} collections ${
            hasActiveFilters ? "found" : "available"
          }`
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

    <div v-if="!numberOfCollections" class="pl-2">
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
      <div class="mb-2">
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
        <hr v-if="index != biobank.collectionDetails.length - 1" />
        <div v-else class="pb-3"></div>
      </div>
    </div>
  </div>
</template>
