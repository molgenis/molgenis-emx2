<template>
  <div>
    <collection-selector
      class="mb-2 float-right"
      v-if="isTopLevelCollection"
      :collectionData="collection"
      :disabled="collection.biobank.withdrawn"/>

    <report-description
      :description="collection.description"
      :maxLength="500"></report-description>

    <!-- collection information -->
    <view-generator :viewmodel="collectionModel.viewmodel" />
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { getCollectionDetails } from '../../utils/templateMapper'
import CollectionSelector from '../buttons/CollectionSelector.vue'
import ReportDescription from '../report-components/ReportDescription.vue'
import ViewGenerator from '../generators/ViewGenerator.vue'

export default {
  name: 'ReportCollectionDetails',
  props: {
    collection: {
      type: Object,
      required: true
    }
  },
  components: {
    CollectionSelector,
    ReportDescription,
    ViewGenerator
  },
  computed: {
    ...mapState(['collectionColumns']),
    collectionModel () {
      return this.collection ? getCollectionDetails(this.collection) : {}
    },
    isTopLevelCollection () {
      return this.collection.parent_collection === undefined
    }
  }
}
</script>
