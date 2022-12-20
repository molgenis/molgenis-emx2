import { defineStore } from 'pinia'
import { i18n } from '../i18n/i18n'
import { initialFilterFacets } from '../filter-config/initialFilterFacets'
import initialCollectionColumns from '../property-config/initialCollectionColumns'
import initialBiobankColumns from '../property-config/initialBiobankColumns'
import { ref } from 'vue'

export const useSettingsStore = defineStore('settingsStore', () => {
  let defaultConfig = ref({
    graphqlEndpoint: 'graphql',
    negotiatorType: 'eric-negotiator',
    collectionColumns: initialCollectionColumns,
    biobankColumns: initialBiobankColumns,
    filterFacets: initialFilterFacets,
    filterMenuInitiallyFolded: false,
    biobankCardShowCollections: true,
    menuHeight: 50,
    pageSize: 12,
    i18n
  })

  const currentPage = ref(1)
  // todo add config from database
  // todo add config management functions

  return { config: defaultConfig, currentPage }
})
