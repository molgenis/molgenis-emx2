/* istanbul ignore file */
import queryEMX2 from '../functions/queryEMX2'
import { useFiltersStore } from '../stores/filtersStore'

/** Async so we can fire and forget for performance. */
async function cache (applyToColumn, filterOptions) {
  const { filterOptionsCache } = useFiltersStore();
  filterOptionsCache[applyToColumn] = filterOptions
}

function retrieveFromCache (applyToColumn) {
  const { filterOptionsCache } = useFiltersStore();
  return filterOptionsCache[applyToColumn] || []
}

/** Configurable array of values to filter out, for example 'Other, unknown' that make no sense to the user. */
function removeOptions (filterOptions, filterFacet) {
  const optionsToRemove = filterFacet.removeOptions

  if (!optionsToRemove || !optionsToRemove.length) return filterOptions

  optionsToRemove.map(option => option.toLowerCase())
  return filterOptions.filter(filterOption => !optionsToRemove.includes(filterOption.text.toLowerCase()))
}

export const customFilterOptions = (filterFacet) => {
  const { applyToColumn, customOptions } = filterFacet
  return () => new Promise((resolve) => {
    const cachedOptions = retrieveFromCache(applyToColumn)

    if (!cachedOptions.length) {
      cache(applyToColumn, customOptions)
      resolve(customOptions)
    }
    else {
       resolve(customOptions)
    }
  });
}

export const genericFilterOptions = (filterFacet) => {
  const { sourceTable, applyToColumn, filterLabelAttribute, filterValueAttribute, sortColumn, sortDirection } = filterFacet

  return () => new Promise((resolve) => {
    const cachedOptions = retrieveFromCache(applyToColumn)

    if (!cachedOptions.length) {
      new queryEMX2('graphql')
        .table(sourceTable)
        .select([filterLabelAttribute, filterValueAttribute])
        .orderBy(sourceTable, sortColumn, sortDirection)
        .execute()
        .then(response => {

          let filterOptions = response[sourceTable].map((row) => { return { text: row[filterLabelAttribute], value: row[filterValueAttribute] } })
          /**  remove unwanted options if applicable */
          filterOptions = removeOptions(filterOptions, filterFacet)

          cache(applyToColumn, filterOptions)
          resolve(filterOptions)
        })
    } else {
      resolve(cachedOptions)
    }
  })
}
