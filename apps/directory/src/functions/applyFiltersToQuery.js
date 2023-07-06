/** sets filters on baseQuery, by reference */
export async function applyFiltersToQuery(
  baseQuery,
  filters,
  facetDetails,
  filterType
) {
  baseQuery.resetAllFilters();
  const activeFilters = Object.keys(filters);

  if (activeFilters.length === 0) return baseQuery;

  for (const filterKey of activeFilters) {
    const filterDetail = facetDetails[filterKey];

    console.log(facetDetails)
    const filterValue = filters[filterKey];

    switch (filterDetail.component) {
      case "StringFilter": {
        /** add filters to Biobanks */
        baseQuery.orWhere("id").like(filterValue);
        baseQuery.orWhere("name").like(filterValue);
        baseQuery.orWhere("acronym").like(filterValue);
        baseQuery.orWhere("collections.id").like(filterValue);
        baseQuery.orWhere("collections.name").like(filterValue);
        baseQuery.orWhere("collections.acronym").like(filterValue);
        baseQuery
          .orWhere("collections.diagnosis_available.name")
          .like(filterValue);
        baseQuery
          .orWhere("collections.diagnosis_available.code")
          .like(filterValue);
        baseQuery
          .orWhere("collections.diagnosis_available.label")
          .like(filterValue);
        baseQuery
          .orWhere("collections.diagnosis_available.definition")
          .like(filterValue);
        baseQuery.orWhere("collections.materials.name").like(filterValue);
        baseQuery.orWhere("collections.materials.label").like(filterValue);

        /** and filter the collections  */
        baseQuery.orFilter("collections.id").like(filterValue);
        baseQuery.orFilter("collections.name").like(filterValue);
        baseQuery.orFilter("collections.acronym").like(filterValue);
        baseQuery
          .orFilter("collections.diagnosis_available.name")
          .like(filterValue);
        baseQuery
          .orFilter("collections.diagnosis_available.code")
          .like(filterValue);
        baseQuery
          .orFilter("collections.diagnosis_available.label")
          .like(filterValue);
        baseQuery
          .orFilter("collections.diagnosis_available.definition")
          .like(filterValue);
        baseQuery.orFilter("collections.materials.name").like(filterValue);
        baseQuery.orFilter("collections.materials.label").like(filterValue);
        break;
      }
      case "ToggleFilter":
      case "CheckboxFilter": {
        const values = Array.isArray(filterValue)
          ? filterValue.map((fv) => fv.value)
          : [filterValue];

        let columns = Array.isArray(filterDetail.applyToColumn)
          ? filterDetail.applyToColumn
          : [filterDetail.applyToColumn];

        for (const column of columns) {
          if (
            filterType[filterDetail.facetIdentifier] === "all" ||
            values.length === 1
          ) {
            if (typeof values[0] === "boolean") {
              for (const value of values) {
                baseQuery.where(column).equals(value);
                baseQuery.filter(column).equals(value);
              }
            } else {
              baseQuery.where(column).like(values);
              baseQuery.filter(column).like(values);
            }
          } else {
            if (typeof values[0] === "boolean") {
              for (const value of values) {
                baseQuery.orWhere(column).equals(value);
                baseQuery.orFilter(column).equals(value);
              }
            } else {
              baseQuery.orWhere(column).orLike(values);
              baseQuery.filter(column).orLike(values);
            }
          }
        }
        break;
      }
      case "OntologyFilter": {
        const values = filterValue.map((fv) => fv.code);
        baseQuery.where(filterDetail.applyToColumn).orLike(values);
        break;
      }
    }
  }
  return baseQuery;
}
