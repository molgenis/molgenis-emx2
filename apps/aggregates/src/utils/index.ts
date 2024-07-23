import { gql, request } from "graphql-request";

/**
 * Map color values to one or more groups
 *
 * @param labels - An array containing one or more categorical values
 * @param palette - An array containing a color scheme
 *
 * @returns Returns an object where the labels are mapped to colors
 */

export function createPalette(labels: string[], palette: string[]): Object {
  const colors = labels.map((label, index) => [label, palette[index]]);
  return Object.fromEntries(colors);
}

/**
 * getUniqueDataTypes
 *
 * Retrieve an array of data types from a dataset
 *
 * @param data a dataset to check (array of objects)
 * @param key name of the key to check
 * @returns string[]
 */
function getUniqueDataTypes(data: Record<string, any>, key: string) {
  const dataTypes: string[] = data.map(
    (row: Record<string, any>) => typeof row[key]
  );
  return [...Array.from(new Set(dataTypes))];
}

/**
 * Sort a dataset by a named property
 *
 * @params data - a dataset to sort; an array of one or more objects
 * @params by - a string containing the name of a key that will be used to sort the dataset
 * @params descending - If True, the dataset will be sorted in reverse order
 */
export function sortData(
  data: object[],
  by: string,
  descending: boolean = false
) {
  const dataType = getUniqueDataTypes(data, by);

  if (dataType.length > 1) {
    throw new Error(
      `Cannot determine sorting type with multiple data types in '${by}'`
    );
  }

  if (dataType[0] === "number") {
    if (descending) {
      return data.sort(
        (current: Record<string, any>, next: Record<string, any>) =>
          current[by] + next[by]
      );
    }
    return data.sort(
      (current: Record<string, any>, next: Record<string, any>) =>
        current[by] - next[by]
    );
  }

  if (dataType[0] === "string") {
    if (descending) {
      return data.sort(
        (current: Record<string, any>, next: Record<string, any>) =>
          current[by] < next[by] ? -1 : 1
      );
    }
    return data.sort(
      (current: Record<string, any>, next: Record<string, any>) =>
        current[by] < next[by] ? 1 : -1
    );
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
export function renameKey(data: object[], oldKey: string, newKey: string) {
  return data.map((row: Record<string, any>) => {
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
  row: Record<string, any>,
  key: string,
  nestedKey: string
): string | number {
  return typeof row[key] === "object" ? row[key][nestedKey] : row[key];
}

/**
 * getGroupByData
 *
 * @param attribute name of the column to query
 * @param filters A GraphQL filters object
 * @returns dataset; array of objects
 */
export async function getGroupByData({
  table,
  attribute,
  filters,
  sub_attribute,
}: {
  table: string;
  attribute: string;
  filters: object;
  sub_attribute: string;
}) {
  const tableId: string = `${table}_groupBy`;
  const tableFilter: string = `${table}Filter`;
  const query = gql`query ($filters: ${tableFilter} ) {
    ${tableId} (filter: $filters) {
      ${attribute} {
        ${sub_attribute}
      }
      _sum {
        n
      }
    }
  }`;
  const variables = { filters: filters };
  const response: Record<string, any> = await request(
    "../api/graphql",
    query,
    variables
  );
  return response[tableId as keyof Response];
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

interface PreppedDataRowIF {
  percent: string;
}

export async function getChartData({
  table,
  sub_attribute = "name",
  labels,
  values,
  groups,
  filters = {},
  nestedLabelKey = "name",
  nestedValueKey = "n",
  nestedGroupKey = "name",
  asPieChartData = false,
}: {
  table: string;
  sub_attribute?: string;
  labels: string;
  values: string;
  groups?: string;
  filters: object;
  nestedLabelKey?: string;
  nestedValueKey?: string;
  nestedGroupKey?: string;
  asPieChartData?: boolean;
}) {
  const data = await getGroupByData({
    table: table,
    attribute: labels,
    filters: filters,
    sub_attribute: sub_attribute,
  });

  const preppedData = data.map((row: object) => {
    const newRow: Record<string, any> = {};
    newRow[labels] = _extractNestedData(row, labels, nestedLabelKey);
    newRow[values] = _extractNestedData(row, values, nestedValueKey);

    if (groups && nestedGroupKey !== null && nestedGroupKey !== "") {
      newRow[groups as string] = _extractNestedData(
        row,
        groups!,
        nestedGroupKey
      );
    }
    return newRow;
  });

  if (asPieChartData) {
    const total = preppedData.reduce((sum: number, row: object) => {
      const value: number = Number(
        _extractNestedData(row, values, nestedValueKey)
      );
      return sum + value;
    }, 0);

    return preppedData
      .map((row: PreppedDataRowIF) => {
        const value = _extractNestedData(row, values, nestedValueKey);
        const val: number = Number(value);
        const percent = ((val / total) * 100).toFixed(2);
        row.percent = percent;
        return row;
      })
      .sort(
        (current: Record<string, any>, next: Record<string, any>) =>
          current[values] + next[values]
      )
      .reduce((pieData: Record<string, any>, row: Record<string, any>) => {
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
export function calculateIncrement(number: number) {
  if (number > 2500) {
    return 1000;
  }

  if (number > 1500 && number < 5000) {
    return 500;
  }
  if (number > 500 && number <= 1500) {
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
export function seqAlongBy(start: number, stop: number, by: number): number[] {
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

/**
 * prepare subselection structure based on attribute name
 * @param key graphql selection
 * @param filter the value to search for
 * @param action search type to perform equals, etc.
 * @returns object
 */
export function gqlPrepareSubSelectionFilter(
  key: string,
  filter: string,
  action: string = "equals"
): object {
  const query = `{"${key}":{"${action}":"${filter}"}}`;
  return JSON.parse(query);
}
