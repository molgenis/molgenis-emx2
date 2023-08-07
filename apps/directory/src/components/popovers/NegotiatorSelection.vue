<template>
  <simple-modal :open="cartVisible" :bodyClass="'w-50'">
    <template v-if="collectionCount > 0">
      <div
        :key="biobankName"
        class="card mb-3 border"
        v-for="(collections, biobankName) in collectionCart"
      >
        <div class="card-header font-weight-bold">
          {{ biobankName }}
        </div>
        <div class="collection-cart">
          <div
            class="card-body d-flex border-bottom"
            :key="`${collection.label}-${index}`"
            v-for="(collection, index) in sortedAlphabetically(collections)"
          >
            <div>
              <span
                v-if="isNonCommercialCollection(collection.value)"
                class="fa-brands fa-creative-commons-nc-eu text-danger non-commercial mr-2"
                title="Not available for commercial use"
              ></span>

              <span> {{ collection.label }}</span>
            </div>
            <div
              class="pl-3 ml-auto"
              @click="
                removeCollectionsFromSelection({
                  biobank: { name: biobankName },
                  collections: [collection],
                  bookmark: bookmark,
                })
              "
            >
              <span
                class="fa fa-times text-bold remove-collection"
                title="Remove collection"
              ></span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <p class="py-3 pl-1">You haven't selected any collections yet.</p>
    </template>
    <template v-slot:modal-footer>
      <div class="bg-primary d-flex align-items-center p-2">
        <button class="btn btn-dark mr-auto" @click="removeAllCollections">
          {{ uiText["remove_all"] }}
        </button>
        <div>
          <span class="text-white font-weight-bold d-block">{{
            modalFooterText
          }}</span>
          <span class="text-white" v-if="anyCollectionNonCommercial">
            <span
              title="Not available for commercial use"
              class="text-white non-commercial mr-1 fa-brands fa-creative-commons-nc-eu"
            ></span>
            {{ selectedNonCommercialCollections }} are non-commercial only
          </span>
        </div>
        <div class="ml-auto">
          <button class="btn btn-dark mr-2" @click="cartVisible = false">
            {{ uiText["close"] }}
          </button>
          <button
            :disabled="collectionCount === 0"
            class="btn btn-secondary ml-auto"
            @click="sendRequest"
          >
            {{ uiText["send_to_negotiator"] }}
          </button>
        </div>
      </div>
    </template>
  </simple-modal>
</template>

<script>
import { useCheckoutStore } from "../../stores/checkoutStore";
import { useCollectionStore } from "../../stores/collectionStore";
import { useSettingsStore } from "../../stores/settingsStore";
import { sortCollectionsByLabel } from "../../functions/sorting";

import SimpleModal from "./SimpleModal.vue";
export default {
  setup() {
    const settingsStore = useSettingsStore();
    const checkoutStore = useCheckoutStore();
    const collectionStore = useCollectionStore();

    return { settingsStore, checkoutStore, collectionStore };
  },
  components: { SimpleModal },
  props: {
    modelValue: {
      type: Boolean,
      required: true,
      default: () => false,
    },
    biobankName: {
      type: String,
      required: false,
    },
    bookmark: {
      type: Boolean,
      required: false,
      default: () => true,
    },
  },
  data: function () {
    return {
      cartVisible: false,
      commercialAvailableCollections: [],
    };
  },
  methods: {
    isNonCommercialCollection(collectionId) {
      const isCommercial = this.commercialAvailableCollections.find(
        (colId) => colId === collectionId
      );

      return !isCommercial;
    },
    removeCollectionsFromSelection(collectionData) {
      this.checkoutStore.removeCollectionsFromSelection(collectionData);
    },
    removeAllCollections() {
      this.cartVisible = false;
      this.checkoutStore.removeAllCollectionsFromSelection({
        bookmark: this.bookmark,
      });
    },
    sortedAlphabetically(collectionArray) {
      return sortCollectionsByLabel(collectionArray);
    },
    sendRequest() {
      this.cartVisible = false;
      this.checkoutStore.sendToNegotiator();
    },
  },
  watch: {
    /** if toggled from outside */
    modelValue(newValue) {
      /** only trigger if different */
      if (this.newValue !== this.cartVisible) {
        this.cartVisible = newValue;
      }
    },
    cartVisible(visibility) {
      /** send back to parent */
      this.$emit("update:modelValue", visibility);
    },
  },
  computed: {
    uiText() {
      return this.settingsStore.uiText;
    },
    collectionCount() {
      return this.checkoutStore.collectionSelectionCount;
    },
    modalFooterText() {
      return `${this.collectionCount} collection(s) selected`;
    },
    collectionCart() {
      return this.checkoutStore.selectedCollections;
    },
    anyCollectionNonCommercial() {
      return this.selectedNonCommercialCollections > 0;
    },

    selectedNonCommercialCollections() {
      let allCollectionIds = [];
      for (const biobank in this.collectionCart) {
        allCollectionIds = allCollectionIds.concat(
          this.collectionCart[biobank].map((col) => col.value)
        );
      }
      const nonCommercialCollections = allCollectionIds.filter(
        (col) => !this.commercialAvailableCollections.includes(col)
      );
      return nonCommercialCollections.length;
    },
  },
  async beforeMount() {
    this.commercialAvailableCollections = await this.collectionStore.getCommercialAvailableCollections();
  },
};
</script>

<style scoped>
.collection-cart > div:last-child {
  border: none !important;
}
</style>
