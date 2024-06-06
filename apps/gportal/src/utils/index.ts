import { filterDataIF } from "../interfaces";

/**
filterData
Filter an array of objects based on inputRefList selection, and then return an arrayof strings based on user-specified property names for use in the Beacon API.

@param data dataset containing one or more rows (array of objects)
@param filters user selected filters (i.e., from InputRefList)
@param attribs an array containing one or more column names in order of preference

@examples
const data = filterData(data=myData, filters=myFilters, attribs=['col1','col2'])

@return array of strings
*/

export function filterData(
  data: filterDataIF[],
  filters: string[],
  attributes: string[]
) {
  return data
    .filter((row) => {
      return filters.map((filterItem) => filterItem.name).includes(row.name);
    })
    .map((row) => {
      return attributes
        .map((attrib) => {
          if (row.hasOwnProperty(attrib)) {
            return row[attrib];
          }
        })
        .join("_");
    });
}
