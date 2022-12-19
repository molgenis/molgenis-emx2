import { defineStore } from 'pinia'
import { i18n } from '../i18n/i18n'
import { initialFilterFacets } from '../filter-config/initialFilterFacets'
import { initialCollectionColumns } from '../property-config/initialCollectionColumns'
import { initialBiobankColumns } from '../property-config/initialBiobankColumns'

export const useGraphqlStore = defineStore('graphqlStore', () => {
  const defaultConfig = {
    negotiatorType: 'eric-negotiator',
    collectionColumns: initialCollectionColumns,
    biobankColumns: initialBiobankColumns,
    filterFacets: initialFilterFacets,
    filterMenuInitiallyFolded: false,
    removeFreemarkerMargin: false,
    biobankCardShowCollections: true,
    menuHeight: 50,
    i18n
  }
  // todo add config from database

  return { defaultConfig }
})
