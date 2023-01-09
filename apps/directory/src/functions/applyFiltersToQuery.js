export function applyFiltersToQuery (baseQuery, filters) {
    baseQuery.resetAllFilters()
    console.log({ filters })
    const activeFilters = Object.keys(filters)

    if (activeFilters.length === 0) return baseQuery

    for (const filterKey of activeFilters) {
        const filterValue = filters[filterKey]
        switch (filterKey) {
            case "search": {
                baseQuery.search(filterValue)
                baseQuery.or("acronym").like(filterValue)
                baseQuery.or("collections", "id").like(filterValue)
                baseQuery.or("collections", "name").like(filterValue)
                baseQuery.or("collections", "acronym").like(filterValue)
                baseQuery.or("collections.diagnosisAvailable", "id").like(filterValue)
                baseQuery.or("collections.diagnosisAvailable", "code").like(filterValue)
                baseQuery.or("collections.diagnosisAvailable", "label").like(filterValue)
                baseQuery.or("collections.diagnosisAvailable", "definition").like(filterValue)
                baseQuery.or("collections.materials", "id").like(filterValue)
                baseQuery.or("collections.materials", "label").like(filterValue)

                console.log(baseQuery.getQuery())
                return baseQuery
            }
        }
    }

}

// TODO: add search on nested column
// TODO: add the properties to the base query, 