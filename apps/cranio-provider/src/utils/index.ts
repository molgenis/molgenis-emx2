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
