/** sets filters on baseQuery, by reference */
export async function applyFiltersToQuery (baseQuery, filters, facetDetails, filterType) {
    baseQuery.resetAllFilters()
    const activeFilters = Object.keys(filters)
    if (activeFilters.length === 0) return baseQuery

    for (const filterKey of activeFilters) {
        const filterDetail = facetDetails[filterKey]
        const filterValue = filters[filterKey]

        switch (filterDetail.component) {
            case "StringFilter": {
                /** add filters to Biobanks */
                baseQuery.orWhere("id").like(filterValue)
                baseQuery.orWhere("name").like(filterValue)
                baseQuery.orWhere("acronym").like(filterValue)
                baseQuery.orWhere("collections.id").like(filterValue)
                baseQuery.orWhere("collections.name").like(filterValue)
                baseQuery.orWhere("collections.acronym").like(filterValue)
                baseQuery.orWhere("collections.diagnosisAvailable.name").like(filterValue)
                baseQuery.orWhere("collections.diagnosisAvailable.code").like(filterValue)
                baseQuery.orWhere("collections.diagnosisAvailable.label").like(filterValue)
                baseQuery.orWhere("collections.diagnosisAvailable.definition").like(filterValue)
                baseQuery.orWhere("collections.materials.name").like(filterValue)
                baseQuery.orWhere("collections.materials.label").like(filterValue)

                /** and filter the collections  */
                baseQuery.filter("collections.id").like(filterValue)
                baseQuery.filter("collections.name").like(filterValue)
                baseQuery.filter("collections.acronym").like(filterValue)
                baseQuery.filter("collections.diagnosisAvailable.name").like(filterValue)
                baseQuery.filter("collections.diagnosisAvailable.code").like(filterValue)
                baseQuery.filter("collections.diagnosisAvailable.label").like(filterValue)
                baseQuery.filter("collections.diagnosisAvailable.definition").like(filterValue)
                baseQuery.filter("collections.materials.name").like(filterValue)
                baseQuery.filter("collections.materials.label").like(filterValue)
                break
            }
            case "CheckboxFilter": {
                const values = filterValue.map(fv => fv.value)

                if (filterType[filterDetail.facetIdentifier] === 'all' || values.length === 1) {
                    if (typeof values[0] === "boolean") {
                        for (const value of values) {
                            baseQuery.where(filterDetail.applyToColumn).equals(value)
                            baseQuery.filter(filterDetail.applyToColumn).equals(value)
                        }
                    }
                    else {
                        baseQuery.where(filterDetail.applyToColumn).like(values)
                        baseQuery.filter(filterDetail.applyToColumn).like(values)
                        baseQuery.subfilter(filterDetail.applyToColumn).like(values)
                    }
                }
                else {
                    if (typeof values[0] === "boolean") {
                        for (const value of values) {
                            baseQuery.orWhere(filterDetail.applyToColumn).equals(value)
                            baseQuery.filter(filterDetail.applyToColumn).equals(value)
                        }
                    }
                    else {
                        baseQuery.where(filterDetail.applyToColumn).orLike(values)
                        baseQuery.filter(filterDetail.applyToColumn).orLike(values)
                        baseQuery.subfilter(filterDetail.applyToColumn).like(values)
                    }
                }
                break
            }
        }
    }
    console.log(baseQuery.getQuery())
    return baseQuery
}

// TODO: add the properties to the base query, 