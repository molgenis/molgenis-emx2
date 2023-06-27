<template>
  <div>
    <div class="d-flex">
      <report-description
        :description="collection.description"
        :maxLength="500"
      ></report-description>
      <collection-selector
        class="mb-2 ml-auto"
        v-if="isTopLevelCollection"
        :biobankData="biobank"
        :collectionData="collection"
        :disabled="collection.biobank.withdrawn"
      />
    </div>

    <!-- collection information -->
    <view-generator :viewmodel="collectionModel.viewmodel" />
  </div>
</template>

<script>
import { getCollectionDetails } from "../../functions/viewmodelMapper";
import CollectionSelector from "../checkout-components/CollectionSelector.vue";
import ReportDescription from "../report-components/ReportDescription.vue";
import ViewGenerator from "../generators/ViewGenerator.vue";

export default {
  name: "ReportCollectionDetails",
  props: {
    biobank: {
      type: Object,
      required: true,
    },
    collection: {
      type: Object,
      required: true,
    },
  },
  components: {
    CollectionSelector,
    ReportDescription,
    ViewGenerator,
  },
  computed: {
    collectionModel() {
      return this.collection ? getCollectionDetails(this.collection) : {};
    },
    isTopLevelCollection() {
      return this.collection.parent_collection === undefined;
    },
  },
};
</script>
