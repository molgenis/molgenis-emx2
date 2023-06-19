<template>
  <b-modal
    hide-header
    id="collectioncart-modal"
    size="lg"
    scrollable
    centered
    body-bg-variant="white"
    footer-bg-variant="warning"
    body-class="pb-0"
    v-model="cartVisible"
    @hide="cartVisible = false">
    <template v-if="collectionCart.length > 0">
      <div
        class="card mb-3 border"
        :key="`${cart.biobankLabel}-${index}`"
        v-for="(cart, index) in collectionCart">
        <div class="card-header font-weight-bold">
          {{ cart.biobankLabel }}
        </div>
        <div class="collection-cart">
          <div
            class="card-body d-flex border-bottom"
            :key="`${collection.label}-${index}`"
            v-for="(collection, index) in cart.collections">
            <div>
              <font-awesome-icon
                title="Not available for commercial use"
                v-if="isNonCommercialCollection(collection.value)"
                class="text-danger non-commercial mr-1"
                :icon="['fab', 'creative-commons-nc-eu']"/>
              <span> {{ collection.label }}</span>
            </div>
            <div class="pl-3 ml-auto">
              <span
                class="fa fa-times text-bold remove-collection"
                title="Remove collection"
                @click="
                  RemoveCollectionsFromSelection({
                    collections: [collection],
                    bookmark: bookmark,
                  })
                "></span>
            </div>
          </div>
        </div>
      </div>
    </template>
    <template v-else>
      <p>You haven't selected any collections yet.</p>
    </template>
    <p v-if="isPodium && !collectionsInPodium.length">
      Sorry, none of the samples are currently in Podium.
    </p>
    <template v-slot:modal-footer>
      <b-button class="btn btn-dark mr-auto" @click="removeAllCollections">{{ uiText['remove_all']}}</b-button>
      <div>
        <span class="text-white font-weight-bold d-block">{{
          modalFooterText
        }}</span>
        <span class="text-white" v-if="selectedNonCommercialCollections > 0">
          <font-awesome-icon
            title="Not available for commercial use"
            class="text-white non-commercial mr-1"
            :icon="['fab', 'creative-commons-nc-eu']"/>
          {{ selectedNonCommercialCollections }} are non-commercial only
        </span>
      </div>
      <div class="ml-auto">
        <b-button class="btn btn-dark mr-2" @click="cartVisible = false">{{ uiText['close'] }}</b-button>
        <b-button
          :disabled="
            (isPodium && !collectionsInPodium.length) ||
            !selectedCollections.length
          "
          class="btn btn-secondary ml-auto"
          @click="sendRequest">{{ negotiatorButtonText }}</b-button>
      </div>
    </template>
  </b-modal>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from 'vuex'
export default {
  props: {
    value: {
      type: Boolean,
      required: true,
      default: () => false
    },
    biobankName: {
      type: String,
      required: false
    },
    bookmark: {
      type: Boolean,
      required: false,
      default: () => true
    }

  },
  data: function () {
    return {
      cartVisible: false
    }
  },
  methods: {
    ...mapMutations(['RemoveCollectionsFromSelection']),
    ...mapActions(['SendToNegotiator']),
    getNameForBiobank (collectionName) {
      const entryInDictionary = this.collectionBiobankDictionary[collectionName]

      if (entryInDictionary) return entryInDictionary

      if (this.biobankReport) {
        return this.biobankReport.label || this.biobankReport.name
      }

      if (this.collectionReport) {
        return this.collectionReport.biobank_label
      }

      return ''
    },
    groupCollectionsByBiobank (collectionSelectionArray) {
      const biobankWithSelectedCollections = []
      collectionSelectionArray.forEach(cs => {
        const biobankLabel = this.getNameForBiobank(cs.value)
        const biobankPresent = biobankWithSelectedCollections.find(
          bsc => bsc.biobankLabel === biobankLabel
        )

        if (biobankPresent) {
          biobankPresent.collections.push(cs)
        } else {
          biobankWithSelectedCollections.push({
            biobankLabel,
            collections: [cs]
          })
        }
      })
      return biobankWithSelectedCollections
    },
    isNonCommercialCollection (collectionId) {
      return this.nonCommercialCollections.indexOf(collectionId) >= 0
    },
    removeAllCollections () {
      this.cartVisible = false
      this.RemoveCollectionsFromSelection({
        collections: this.currentSelectedCollections,
        bookmark: this.bookmark
      })
    },
    sendRequest () {
      this.cartVisible = false
      this.SendToNegotiator()
    }
  },
  watch: {
    /** if toggled from outside */
    value (newValue) {
      /** only trigger if different */
      if (this.newValue !== this.cartVisible) {
        this.cartVisible = newValue
      }
    },
    cartVisible (visibility) {
      /** send back to parent */
      this.$emit('input', visibility)
    }
  },
  computed: {
    ...mapGetters([
      'collectionsInPodium',
      'selectedCollections',
      'selectedNonCommercialCollections',
      'collectionBiobankDictionary',
      'uiText'
    ]),
    ...mapState([
      'isPodium',
      'nonCommercialCollections',
      'biobankReport',
      'collectionReport'
    ]),
    modalFooterText () {
      const collectionCount = this.isPodium
        ? this.collectionsInPodium.length
        : this.selectedCollections.length
      return this.isPodium
        ? `${collectionCount} collection(s) present in Podium`
        : `${collectionCount} collection(s) selected`
    },
    negotiatorButtonText () {
      return this.isPodium ? this.uiText.send_to_podium : this.uiText.send_to_negotiator
    },
    currentSelectedCollections () {
      return this.isPodium ? this.collectionsInPodium : this.selectedCollections
    },
    collectionCart () {
      return this.groupCollectionsByBiobank(this.currentSelectedCollections)
    }
  }
}
</script>

<style scoped>
.collection-cart > div:last-child {
  border: none !important;
}
</style>
