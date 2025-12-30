import type {
  ResultSetIF,
  OntologyDataIF,
  BeaconResultsIF,
} from "../interfaces/beacon";

/**
filterData
Filter an array of objects based on inputRefList selection, and then return an arrayof strings based on user-specified property names for use in the Beacon API.

@param data dataset containing one or more rows (array of objects)
@param filters user selected filters (i.e., from InputRefList)
@param attribs an array containing one or more column names in order of preference

@examples
const data = filterData(data=myData, filters=myFilters, attribs=['col1','col2'])

@return array of strings in curie syntax
*/

export function filterData(
  data: OntologyDataIF[],
  filters: string[],
  attributes: string[]
) {
  return data
    .filter((row: OntologyDataIF) => filters.includes(row.name))
    .map((row: OntologyDataIF) => {
      return attributes
        .map((attrib: string) => {
          if (row.hasOwnProperty(attrib)) {
            return row[attrib as keyof OntologyDataIF];
          }
        })
        .join("_");
    });
}

/**
 * prepare beacon response
 * In the response object, unpack the results sets and transform the data for use in the DataTable component.
 *
 * @param resultsSets an array of objects that contain beacon hits
 *    found in `response.data.resultsets`
 */

export function transformBeaconResultSets(
  data: ResultSetIF[]
): BeaconResultsIF[] {
  return data.map((row: ResultSetIF) => {
    return {
      schema: row.id,
      table: row.setType,
      status: row.exists ? "Available" : "Unavailable",
      count: row.resultsCount,
    };
  });
}
