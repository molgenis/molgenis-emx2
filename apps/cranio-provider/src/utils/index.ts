import type { IKeyValuePair } from "../interfaces/index";

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
