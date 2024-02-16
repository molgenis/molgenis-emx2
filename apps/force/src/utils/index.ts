import { gql, request } from "graphql-request";

/**
 * Map color values to one or more groups
 *
 * @param labels - An array containing one or more categorical values
 * @param palette - An array containing a color scheme
 *
 * @returns Returns an object where the labels are mapped to colors
 */

export function createPalette(labels: Array, palette: Array): Object {
  const colors = labels.map((label, index) => [label, palette[index]]);
  return Object.fromEntries(colors);
}

/**
 * Sort a dataset by a named property
 *
 * @params data - a dataset to sort; an array of one or more objects
 * @params by - a string containing the name of a key that will be used to sort the dataset
 * @params descending - If True, the dataset will be sorted in reverse order
 */
export function sortData(
  data: Array,
  by: String,
  descending: Boolean = false
): Array {
  const dataType = [...new Set(data.map((row) => typeof row[by]))];
  if (dataType.length > 1) {
    throw new Error(
      `Cannot determine sorting type with multiple data types in '${by}'`
    );
  }

  if (dataType[0] === "number") {
    if (descending) {
      return data.toSorted((current, next) => current[by] + next[by]);
    }
    return data.toSorted((current, next) => current[by] - next[by]);
  }

  if (dataType[0] === "string") {
    if (descending) {
      return data.toSorted((current, next) =>
        current[by] < next[by] ? -1 : 1
      );
    }
    return data.toSorted((current, next) => (current[by] < next[by] ? 1 : -1));
  }
}

/**
 * Rename a property in a dataset (i.e., array of objects)
 *
 * @param data an array of objects
 * @param oldKey name of the property to rename
 * @param newKey name to substitute
 *
 * @param an array of objects
 */
export function renameKey(data: Array, oldKey: String, newKey: String) {
  return data.map((row) => {
    if (Object.hasOwn(row, oldKey)) {
      row[newKey] = row[oldKey];
      delete row[oldKey];
    }
    return row;
  });
}

/**
 * Flattens a nested object
 * @param row
 * @param key
 * @param nestedKey
 * @returns nested value string, number, etc.
 */
function _extractNestedData(
  row: Object,
  key: String,
  nestedKey: String
): String {
  return typeof row[key] === "object" ? row[key][nestedKey] : row[key];
}

/**
 *
 * @param attribute name of the column to query
 * @param filters A GraphQL filters object
 * @returns dataset; array of objects
 */
export async function getData(attribute: String, filters: Object) {
  const query = gql`query ($filters: ClinicalDataFilter ) {
    ClinicalData_groupBy (filter: $filters) {
      ${attribute} {
        name
      }
      _sum {
        n
      }
    }
  }`;
  const variables = { filters: filters };
  const response = await request("../api/graphql", query, variables);
  return response.ClinicalData_groupBy;
}

/**
 * Transform a dataset for use in one of the d3 charts. The input dataset will be reduced
 * to the desired columns and nested objects will be flattened. Additional transformations
 * can be applied for different chart types.
 *
 * @param labels - the name of the key that contains the labels that describes the values
 * @param values - the name of the key that contains the values
 * @param groups - the name of column used to group records
 * @param filters - object containing filters to pass on to the graphql query
 * @param nestedLabelKey - If the label is an object, the nested key will be targeted
 * @param nestedValueKey - If the value is an object, the nested key will be targeted
 * @param nestedGroupKey - If the group is an object, the nested key will be targeted
 * @param asPieChartData - If True, the dataset will be transformed for use in a pie chart
 *
 * @returns dataset for use in a d3 chart; array of objects or a single object
 */

interface ChartData {
  labels: String;
  values: String;
  groups?: String;
  filters: Object;
  nestedLabelKey: String;
  nestedValueKey: String;
  nestedGroupKey: String;
  asPieChartData: Boolean;
}

export async function getChartData({
  labels,
  values,
  groups = null,
  filters = {},
  nestedLabelKey = "name",
  nestedValueKey = "n",
  nestedGroupKey = "name",
  asPieChartData = false,
}): ChartData {
  const data = await getData(labels, filters);

  const preppedData = data.map((row) => {
    const newRow = {};
    newRow[labels] = _extractNestedData(row, labels, nestedLabelKey);
    newRow[values] = _extractNestedData(row, values, nestedValueKey);

    if (groups && (nestedGroupKey !== null) | (nestedGroupKey !== "")) {
      newRow[groups] = _extractNestedData(row, groups, nestedGroupKey);
    }
    return newRow;
  });

  if (asPieChartData) {
    const total = preppedData.reduce((sum, row) => {
      const value = _extractNestedData(row, values, nestedValueKey);
      return sum + value;
    }, 0);

    return preppedData
      .map((row) => {
        const value = _extractNestedData(row, values, nestedValueKey);
        row["percent"] = ((value / total) * 100).toFixed(2);
        return row;
      })
      .reduce((pieData, row) => {
        pieData[row[labels]] = row[values];
        return pieData;
      }, {});
  }

  return preppedData;
}

/**
 * Evaluate a number and return a suitable increment
 * @param number a number to evaluate
 * @returns number
 */
export function calculateIncrement(number: Number) {
  if (number > 1000) {
    return 1000
  }
  if (number > 500 && number <= 1000) {
    return 250;
  }
  
  if (number > 100 && number <= 500) {
    return 100;
  }
  
  if (number > 50 && number <= 100) {
    return 25;
  }
  
  if (number > 10 && number <= 50) {
    return 10;
  }
  
  return 2;
}

/**
 * Generate a sequence between two numbers by a specific interval
 * @param start the number to start the sequence
 * @param stop the number to stop the sequence
 * @param by the increment between each item in the sequence
 * @returns an array of numbers
 */
export function seqAlongBy(start: Number, stop: Number, by: Number): Array {
  return Array.from(
    { length: (stop - start) / by + 1 },
    (_, i) => start + i * by
  );
}

/**
 * Check to see if number is between 0 and 1
 * @param value a value that is between 0 and 1
 * @returns boolean
 */
export function validateNumRange(value: number) {
  return value >= 0 && value <= 1;
}
