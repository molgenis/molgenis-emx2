import type { IKeyValuePair } from "../types/index";
import type { IChartData } from "../types/schema";

/**
 * @name asKeyValuePairs
 * @description Convert an array of objects to an object of key-values
 *
 * @param data array of objects
 * @param key name of the column containing the values that will be used as keys
 * @param value name of the column containing the values to map to the new keys
 *
 * @returns object of key-value pairs
 */
export function asKeyValuePairs(
  data: any,
  key: string,
  value: string
): IKeyValuePair {
  const values = data.map((row: Record<string, any>) => {
    return [row[key], row[value]];
  });
  return Object.fromEntries(values);
}

/**
 * @name sum
 * @description calculate the sum of a key in an array of objects
 *
 * @param data input dataset; an array of objects
 * @param key name of the column containing the values to sum
 * @returns number
 */
export function sum(data: any, key: string): number {
  return data.reduce(
    (sum: number, row: Record<string, any>) =>
      row[key] !== null || row[key] !== "" ? sum + row[key] : null,
    0
  );
}

/**
 * @name sumObject
 * @description calculate the sum of values in an object
 *
 * @param data input dataset; an object containing one or more key-value pairs
 *
 * @returns number
 */
export function sumObjectValues(data: IKeyValuePair): number {
  const values: number[] = Object.keys(data).map((key: string) => {
    if (data[key] !== null || data[key] !== "") {
      return parseInt(data[key]);
    }
  }) as number[];
  return values.reduce((acc: number, value: number) => acc + value, 0);
}

/**
 * @name uniqueValues
 * @description Return an array of unique values from a specific property in a dataset
 *
 * @param data array of objects
 * @param key name of the key that contains the values you want to reduce
 *
 * @example
 * ```js
 * const data = [{value: "cat"}, {value: "mouse"}, {value: "cat"}, {value: "dog"}];
 * uniqueValues(data, "value")
 * // ["cat", "dog", "mouse"]
 * ```
 *
 * @returns array of unique values in alphabetical order
 */
export function uniqueValues(data: any, key: string): string[] {
  const values = data.map((row: Record<string, any>) => row[key]);
  return Array.from(new Set(values)).sort() as string[];
}

export function uniqueAgeGroups(data: any, key: string): string[] {
  const values = uniqueValues(data, key);
  return values.sort((a: string, b: string) => {
    return b.charCodeAt(0) - a.charCodeAt(0);
  });
}

/**
 * @name ernCenterPalette
 * @description color palette for charts that show ERN and center comparisons
 */
export const ernCenterPalette: IKeyValuePair = {
  ERN: "#66c2a4",
  "Your center": "#3f6597",
};

/**
 * @name sortByDataPointName
 *
 * @param data dataset to sort (IChartData[] from ICharts)
 * @returns sorted dataset by "dataPointName" column
 */
export function sortByDataPointName(data: IChartData[]) {
  return data.sort((a: IChartData, b: IChartData) => {
    return a.dataPointName?.localeCompare(b.dataPointName as string) as number;
  });
}
