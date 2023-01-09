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
                baseQuery.where("acronym").like(filterValue)
                baseQuery.where("collections", "id").like(filterValue)
                baseQuery.where("collections", "name").like(filterValue)
                baseQuery.where("collections", "acronym").like(filterValue)
                // baseQuery.filter("collections.diagnosisAvailable", "id").like(filterValue)
                // baseQuery.filter("collections.diagnosisAvailable", "code").like(filterValue)
                // baseQuery.filter("collections.diagnosisAvailable", "label").like(filterValue)
                // baseQuery.filter("collections.diagnosisAvailable", "definition").like(filterValue)
                // baseQuery.filter("collections.materials", "id").like(filterValue)
                // baseQuery.filter("collections.materials", "label").like(filterValue)
                console.log(baseQuery.getQuery())
                return baseQuery
            }
        }
    }

}

// TODO: add search on nested column
// TODO: add the properties to the base query, 