import type { IChartData } from "../types/schema";
import { uniqueValues } from "./index";

interface INumberLabel {
  value: number;
  label: string;
}

/**
 * @name getUniqueAgeRanges
 * @description Age ranges are formated like so "10-19 years". We need reformat the values to find the unique cases and sort them in the correct order.
 *
 * @param data input data (IChartData[] from IChart)
 * @param key name of the column containing the age data
 *
 * @returns array of unique age ranges
 */
export function getUniqueAgeRanges(data: IChartData[], key: string): string[] {
  const uniqueAges: string[] = uniqueValues(data, key);
  const ranges: INumberLabel[] = uniqueAges
    .map((value: string) => {
      return {
        value: parseInt(value.split(/(-)/)[0]),
        label: value,
      };
    })
    .sort((a: INumberLabel, b: INumberLabel) => a.value - b.value);
  return ranges.map((row: INumberLabel) => row.label);
}
