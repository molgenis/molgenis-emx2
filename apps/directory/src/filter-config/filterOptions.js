/* istanbul ignore file */
import api from '@molgenis/molgenis-api-client'
import store from '../store'
import { encodeRsqlValue, transformToRSQL } from '@molgenis/rsql'
import { isCodeRegex } from '../../src/store/helpers'
import { createTextSearchQuery } from '.'

// Async so we can fire and forget for performance.
async function cache (filterData) {
  store.commit('SetFilterOptionDictionary', filterData)
}

function retrieveFromCache (filterName) {
  return store.state.filterOptionDictionary[filterName] ?? []
}

// Configurable array of values to filter out, for example 'Other, unknown' that make no sense to the user.
function removeOptions (filterOptions, filterFacet) {
  const optionsToRemove = filterFacet.removeOptions

  if (!optionsToRemove || !optionsToRemove.length) return filterOptions

  optionsToRemove.map(option => option.toLowerCase())
  return filterOptions.filter(filterOption => !optionsToRemove.includes(filterOption.text.toLowerCase()))
}

function checkForBookmarkFilter (filterName, filterOptions) {
  if (!store.state.diagnosisAvailableFetched) {
    /** If we have a cold start with a bookmark
     * we need to have the label for the selected filter
     */
    const activeDiagnosisFilter = store.getters.activeFilters[filterName]

    if (activeDiagnosisFilter) {
      let options = []
      for (const activeFilter of activeDiagnosisFilter) {
        const optionToCache = filterOptions.filter(option => option.value === activeFilter)
        if (optionToCache) {
          options = options.concat(optionToCache)
        }
      }
      if (options.length) {
        cache({ filterName, filterOptions: options })
      }
    }
  } else {
    /** we can just add the newly searched */
    store.commit('SetFilterOptionDictionary', { filterName, filterOptions })
  }
}

export const genericFilterOptions = (filterFacet) => {
  const { tableName, name, filterLabelAttribute, queryOptions } = filterFacet
  return () => new Promise((resolve) => {
    const cachedOptions = retrieveFromCache(name)

    if (!cachedOptions.length) {
      api.get(`/api/v2/${tableName}${queryOptions || ''}`).then(response => {
        let filterOptions = response.items.map((obj) => { return { text: obj[filterLabelAttribute] || obj.label || obj.name, value: obj.id } })

        // remove unwanted options if applicable
        filterOptions = removeOptions(filterOptions, filterFacet)

        cache({ filterName: name, filterOptions })
        resolve(filterOptions)
      })
    } else {
      resolve(cachedOptions)
    }
  })
}

/** Specific logic for diagnosis available filter */
const createDiagnosisLabelQuery = (query) => transformToRSQL(createTextSearchQuery('label', query, true))
const createDiagnosisCodeQuery = (query) => transformToRSQL({ selector: 'code', comparison: '=like=', arguments: query.toUpperCase() })
/** */

export const diagnosisAvailableFilterOptions = (tableName, filterName) => {
  // destructure the query part from the multi-filter
  return ({ query, queryType }) => new Promise((resolve) => {
    let url = `/api/v2/${tableName}`

    if (query) {
      // initial load, values are id's
      if (queryType === 'in') {
        url = `${url}?q=${encodeRsqlValue(`id=in=(${query})`)}`
      } else if (isCodeRegex.test(query)) {
        url = `${url}?q=${encodeRsqlValue(createDiagnosisCodeQuery(query))}&sort=code`
      } else {
        url = `${url}?q=${encodeRsqlValue(createDiagnosisLabelQuery(query))}`
      }
    }

    api.get(url).then(response => {
      const filterOptions = response.items.map((obj) => { return { text: `[ ${obj.code} ] - ${obj.label || obj.name}`, value: obj.id } })

      checkForBookmarkFilter(filterName, filterOptions)
      cache({ filterName, filterOptions })
      resolve(filterOptions)
    })
  })
}

export const collaborationTypeFilterOptions = () => {
  const filterOptions = [{ text: 'Commercial use', value: 'true' }, { text: 'Non-commercial use only', value: 'false' }]
  cache({ filterName: 'commercial_use', filterOptions })
  return () => new Promise((resolve) => {
    resolve(filterOptions)
  })
}
