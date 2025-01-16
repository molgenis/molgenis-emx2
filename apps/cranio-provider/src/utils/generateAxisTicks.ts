import type { IAxisTickData, IKeyValuePair } from "../types/index";
import type { IChartData } from "../types/schema"; 

/**
 * @name generateAxisTicks
 * @description autogenerate the axis ticks and max value from a dataset
 *
 * @param data dataset containing the values you want to use to generate axis ticks
 * @param key name of the column that contains the values
 *
 * @returns an object containing the limit (i.e., max value) and ticks
 */
export function generateAxisTickData(
  data: IChartData[],
  key: string
): IAxisTickData {
  const max = Math.max(...data.map((row: IChartData) => row[key]));
  const limit = max < 10 ? 10 : max;
  const interval = calculateInterval(limit);
  const limitAdjusted = Math.ceil(limit / interval) * interval;
  const ticks = seqAlongBy(0, limitAdjusted, interval);
  return { limit: limitAdjusted, ticks: ticks };
}

/**
 * @name seqAlongBy
 * @description generate a sequence of numbers by interval
 *
 * @param start number to start the sequence (e.g., 0)
 * @param stop number to stop the sequence (e.g., 25)
 * @param by number used to specify the interval in the sequence (e.g., 5)
 *
 * @example
 * ```js
 * seqAlongBy(0, 25, 5)
 * // [0, 5, 10, 15, 25]
 * ```
 *
 * @returns an array of numbers in numerical order
 */
export function seqAlongBy(
  start: number,
  stop: number,
  by: number = 1
): number[] {
  return Array.from(
    { length: (stop - start) / by + 1 },
    (_, i) => start + i * by
  );
}

/**
 * @name calculateInterval
 * @description determine the appropriate interval to use based on a given value. In a column in a dataset, this is usually the maximum value (or minimum value if dealing with negative values). This value is also important in the visualization component as it will help adjust the max value so that there are even ticks along the axis. For example, if your max value is 86, it will adjust the max axis value as 100 and generate the ticks accordingly.
 *
 * @param value a number used to determine an interval.
 *
 * @example
 * ```js
 * const data = [{value: 12}, {value: 75}, {value: 90}];
 * const maxValue = Math.max(...data.map(row => row.value));
 * calculateInterval(maxValue);
 * // maxValue: 90
 * // interval: 25
 * ```
 *
 * @returns a number
 */
export function calculateInterval(value: number): number {
  if (value > 5000) {
    return 1000;
  } else if (value > 1500 && value < 5000) {
    return 500;
  } else if (value > 500 && value <= 1500) {
    return 250;
  } else if (value > 100 && value <= 500) {
    return 100;
  } else if (value > 50 && value <= 100) {
    return 25;
  } else if (value > 10 && value <= 50) {
    return 10;
  } else if (value > 5 && value <= 10) {
    return 2;
  } else {
    return 1;
  }
}
