export async function applyFiltersToQuery (baseQuery, filters) {
    baseQuery.resetAllFilters()
    console.log({ filters })
    const activeFilters = Object.keys(filters)

    if (activeFilters.length === 0) return baseQuery

    for (const filterKey of activeFilters) {
        const filterValue = filters[filterKey]
        switch (filterKey) {
            case "search": {
                baseQuery.search(filterValue)
                baseQuery.orWhere("id").like(filterValue)
                baseQuery.orWhere("name").like(filterValue)
                baseQuery.orWhere("acronym").like(filterValue)
                baseQuery.orWhere("collections", "id").like(filterValue)
                baseQuery.orWhere("collections", "name").like(filterValue)
                baseQuery.orWhere("collections", "acronym").like(filterValue)
                baseQuery.customWhere({ column: "collections", subcolumn: "diagnosisAvailable", clause: "{ foo } ", type: " _or" })
                // baseQuery.orWhere("collections.diagnosisAvailable.id").like(filterValue)
                // baseQuery.orWhere("collections.diagnosisAvailable.code").like(filterValue)
                // baseQuery.orWhere("collections.diagnosisAvailable.label").like(filterValue)
                // baseQuery.orWhere("collections.diagnosisAvailable.definition").like(filterValue)
                baseQuery.orWhere("collections.materials.id").like(filterValue)
                baseQuery.orWhere("collections.materials.label").like(filterValue)
                console.log(baseQuery.getQuery())
                return baseQuery
            }

        }

        return baseQuery
    }

}

// TODO: add search on nested column
// TODO: add the properties to the base query, 