
<template>
  <div>
    <input
      id="select-deselect-all"
      type="checkbox"
      class="add-to-cart"
      @change="handleCollectionStatus"
      :checked="allCollectionsSelected"
      hidden/>
    <label
      class="add-to-cart-label btn btn-outline-success m-0"
      for="select-deselect-all">
      <span>{{ uiText["select_all_collections"] }}</span>
    </label>
    <label
      class="remove-from-cart-label btn btn-danger m-0"
      for="select-deselect-all">
      <span>{{ uiText["deselect_all_collections"] }}</span>
    </label>
  </div>
</template>

<script>
import { mapActions, mapGetters, mapMutations } from 'vuex'

export default {
  name: 'CollectionSelectAll',
  props: {
    bookmark: {
      type: Boolean,
      required: false,
      default: false
    }
  },
  methods: {
    ...mapActions(['AddCollectionsToSelection']),
    ...mapMutations(['RemoveCollectionsFromSelection']),
    handleCollectionStatus (event) {
      const { checked } = event.target
      const collectionData = {
        collections: this.foundCollectionsAsSelection,
        bookmark: this.bookmark
      }

      if (checked) {
        this.AddCollectionsToSelection(collectionData)
      } else {
        this.RemoveCollectionsFromSelection(collectionData)
      }
    }
  },
  computed: {
    ...mapGetters([
      'allCollectionsSelected',
      'foundCollectionsAsSelection',
      'uiText'
    ])
  }
}
</script>

<style scoped>
.btn:hover {
  cursor: pointer;
}

.add-to-cart:checked ~ .add-to-cart-label {
  display: none;
}

.remove-from-cart-label {
  display: none;
}

.add-to-cart:checked ~ .remove-from-cart-label {
  display: inline-block;
}
</style>
