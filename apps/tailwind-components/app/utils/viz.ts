import type {
  NumericAxisTickData,
  NewNumericAxisGeneratorProps,
  NewCategoricalAxisGeneratorProps,
  LegendPosition,
  DatasetRow,
} from "../../types/viz";

import { select, scaleBand, scaleLinear } from "d3";
const d3 = { select, scaleLinear, scaleBand };

/**
 * @name setChartLegendLayoutCss
 * @description Sets a chart component layout of title, legend, chart
 *
 * @param legendIsEnabled bool that indicates the the legenis rendered
 * @param legendPosition "top" or "bottom"
 * @returns a CSS string
 */
export function setChartLegendLayoutCss(
  legendIsEnabled: boolean,
  legendPosition?: LegendPosition
): string {
  if (legendIsEnabled && legendPosition) {
    return `chart_layout_with_legend_${legendPosition}`;
  }
  return `chart_layout_default`;
}

/**
 * @name breakXAxisLabels
 * @description If defined, axis labels are wrapped at a specific character(s)
 *
 * @param svg a d3 selection of an svg element
 * @param breakXAxisLabelsAt a string containing a character(s) to break the text
 */
// splits x-axis labels at a specific character by creating two span elements
export function breakXAxisLabels(svg: any, breakXAxisLabelsAt: string) {
  const separator: string = breakXAxisLabelsAt;
  const axisText = svg.selectAll("g.chart-area g.axes .x-axis .tick text");
  axisText.call(
    (labels: d3.Selection<SVGTextElement, {}, HTMLElement, any>) => {
      labels.each(function () {
        const node = d3.select(this);
        const valueArray: string[] = node.text().split(separator);
        node.text("");
        valueArray.forEach((value: string) => {
          node.append("tspan").attr("x", 0).attr("dy", "1em").text(value);
        });
      });
    }
  );
}

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
  data: DatasetRow[],
  key: string
): NumericAxisTickData {
  const values: number[] = [
    ...data.map((row: Record<string, any>) => row[key] as number),
  ];
  const max = Math.max(...values);
  const min = Math.min(...values);
  const limit = max < 10 ? 10 : max;
  const interval = calculateInterval(limit);
  const limitAdjusted = Math.ceil(limit / interval) * interval;
  const ticks = seqAlongBy(0, limitAdjusted, interval);
  return { limit: limitAdjusted, ticks: ticks, max: max, min: min };
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
  } else if (value > 1500 && value <= 5000) {
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

/**
 * @name newNumericAxisGenerator
 * @description Creates a d3 linear scale which can be used to convert bind data point to svg coordinates
 *
 * @param domainMin the minimum value in the numeric axis (typically 0)
 * @param domainLimit the maximum value in the numeric axis (max of yvar)
 * @param rangeStart the first number used to sequence (usually chart height)
 * @param rangeEnd the last number in the sequence (usually 0)
 *
 * @link https://d3js.org/d3-scale/linear#linear_domain
 * @link https://d3js.org/d3-array/ticks#range
 *
 * @returns d3.scaleLinear
 */
export function newNumericAxisGenerator({
  domainMin = 0,
  domainLimit,
  rangeStart,
  rangeEnd = 0,
}: NewNumericAxisGeneratorProps) {
  return d3
    .scaleLinear()
    .domain([domainMin, domainLimit])
    .range([rangeStart, rangeEnd])
    .nice();
}

/**
 * @name newCategoricalAxisGenerator
 * @description Creates an axis generator for string/categorical axes
 *
 * @param domains: an array of values to bin data
 * @param rangeStart the first number in the range (usually 0)
 * @param rangeEnd the final number in the range (usually width of the chart)
 *
 * @returns d3.scaleBand
 */
export function newCategoricalAxisGenerator({
  domains,
  rangeStart = 0,
  rangeEnd,
}: NewCategoricalAxisGeneratorProps) {
  return d3
    .scaleBand()
    .range([rangeStart, rangeEnd])
    .domain(domains)
    .paddingInner(0.25)
    .paddingOuter(0.25)
    .align(0.5)
    .round(true);
}
