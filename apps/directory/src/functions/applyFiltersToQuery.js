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

    const filterValue = filters[filterKey];

    switch (filterDetail.component) {
      case "StringFilter": {
        if (filterKey === "search" && filterValue) {
          baseQuery.searchValue = filterValue;
          baseQuery.searchFieldsByProperty = {
            root: [
              "id",
              "name",
              "acronym",
              "collections.id",
              "collections.name",
              "collections.acronym",
              "collections.diagnosis_available.name",
              "collections.diagnosis_available.code",
              "collections.diagnosis_available.label",
              "collections.diagnosis_available.definition",
              "collections.materials.name",
              "collections.materials.label",
            ],
            collections: [
              "id",
              "name",
              "acronym",
              "biobank.name",
              "diagnosis_available.name",
              "diagnosis_available.code",
              "diagnosis_available.label",
              "diagnosis_available.definition",
            ],
          };
        } else {
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
          baseQuery.orFilter("collections.biobank.name").like(filterValue);
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
        }

        /** cant search in the search box on any filter that is set to 'adaptive' because the items will not show. */
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
              baseQuery.where(column).in(values);
              baseQuery.filter(column).in(values);
            }
          } else {
            if (typeof values[0] === "boolean") {
              for (const value of values) {
                baseQuery.orWhere(column).equals(value);
                baseQuery.orFilter(column).equals(value);
              }
            } else {
              baseQuery.orWhere(column).in(values);
              baseQuery.orFilter(column).in(values);
            }
          }
        }
        break;
      }
      case "OntologyFilter": {
        const values = filterValue.map((filterValue) => filterValue.code);
        if (
          filterType[filterDetail.facetIdentifier] === "all" ||
          values.length === 1
        ) {
          baseQuery.where(filterDetail.applyToColumn).in(values);
          baseQuery.filter(filterDetail.applyToColumn).in(values);
        } else {
          baseQuery.orWhere(filterDetail.applyToColumn).in(values);
          baseQuery.orFilter(filterDetail.applyToColumn).in(values);
        }
        break;
      }
    }
  }
  return baseQuery;
}
