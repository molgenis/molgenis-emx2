<template>
  <div>
    <application-header-partial-view />
    <biobank-cards-partial-view />
  </div>
</template>

<script>
import ApplicationHeaderPartialView from "../components/partial-views/ApplicationHeaderPartialView.vue";
import BiobankCardsPartialView from "../components/partial-views/BiobankCardsPartialView.vue";
// import { mapGetters, mapActions, mapState, mapMutations } from 'vuex'
// import { createBookmark } from '../utils/bookmarkMapper'
// import ApplicationHeader from '../components/ApplicationHeader.vue'

export default {
  name: "biobank-explorer",
  components: {
    ApplicationHeaderPartialView,
    BiobankCardsPartialView,
  },
  // computed: {
  //   ...mapGetters([
  //     'rsql',
  //     'biobankRsql',
  //     'activeFilters',
  //     'selectedCollections'
  //   ]),
  //   ...mapState(['isPodium', 'filters'])
  // },
  // watch: {
  //   rsql: {
  //     immediate: true,
  //     handler: 'GetCollectionInfo'
  //   },
  //   biobankRsql: {
  //     immediate: true,
  //     handler: 'QueryBiobanks'
  //   },
  //   isPodium: {
  //     immediate: true,
  //     handler: 'GetPodiumCollections'
  //   }
  // },
  // methods: {
  //   ...mapMutations(['MapQueryToState']),
  //   ...mapActions([
  //     'GetCollectionInfo',
  //     'QueryBiobanks',
  //     'GetPodiumCollections'
  //   ])
  // },
  // mounted () {
  //   /** check if collections have been added off-screen. */
  //   createBookmark(this.filters, this.selectedCollections)
  // }
};
</script>

<style>
.non-commercial .fa-times {
  font-size: 1em;
}

.remove-collection:hover,
.btn:hover,
#select-all-label:hover {
  cursor: pointer;
}
</style>
