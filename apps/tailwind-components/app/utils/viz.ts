import { vizDelay, vizDurations } from "../utils/cms/constants";

import type {
  NumericAxisTickData,
  NewNumericAxisGeneratorProps,
  NewCategoricalAxisGeneratorProps,
  LegendPosition,
  SelectChartMouseOverProps,
} from "../../types/viz";

import type { IChartData } from "../../types/cms";

import { select, scaleBand, scaleLinear } from "d3";
const d3 = { select, scaleLinear, scaleBand };

/**
 * @name setChartLegendLayoutCss
 * @description Sets a chart component layout of title, legend, chart
 *
 * @param legendIsEnabled bool that indicates the legend is rendered
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
  data: IChartData[],
  key: string,
  minValue?: number,
  maxValue?: number
): NumericAxisTickData {
  const values: number[] = [
    ...data.map((row: Record<string, any>) => row[key] as number),
  ];
  const max: number = maxValue || Math.max(...values);
  let min: number = minValue || Math.min(...values);
  if (min > 0) {
    min = 0;
  }
  const ticks = d3.scaleLinear().domain([min, max]).nice().ticks();
  const limit = Math.max(...ticks);
  return { limit: limit, ticks: ticks, max: max, min: min };
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
 * @param paddingInner the amount of space inbetween bars (0-1)
 * @param paddingOuter the amount of space before the first bar and after the last (0-1)
 *
 * @returns d3.scaleBand
 */
export function newCategoricalAxisGenerator({
  domains,
  rangeStart = 0,
  rangeEnd,
  paddingInner = 0.25,
  paddingOuter = 0.25,
}: NewCategoricalAxisGeneratorProps) {
  return d3
    .scaleBand()
    .range([rangeStart, rangeEnd])
    .domain(domains)
    .paddingInner(paddingInner)
    .paddingOuter(paddingOuter)
    .align(0.5)
    .round(true);
}

/**
 * @name asDataObject
 * @description Converts array of objects into an object of key-value pairs.
 * This is useful for components where the input dataset must be an
 * object (e.g., PieChart, Highlights, etc.).
 *
 * @param data input dataset to convert
 * @param labelKey name of the key that has the labels
 * @param valueKey name of the key that has the values
 * @param sortData If true, data will be sorted by value
 *
 * @returns Record<string,number>
 */
export function asDataObject(
  data: IChartData[],
  labelKey: string,
  valueKey: string,
  sortData?: boolean
): Record<string, number> {
  let result = data.map((row: IChartData) => [
    row[labelKey as keyof IChartData],
    row[valueKey as keyof IChartData],
  ]);
  if (sortData) {
    result = result.sort((current, next) => {
      // @ts-expect-error
      if (current[1] < next[1]) {
        return 1;
      } else {
        return -1;
      }
    });
  }
  return Object.fromEntries(result);
}

/**
 * @name selectMouseOverElements
 * @description selects and returns svg element required for hover events
 *
 * @param row chart data
 * @param isGroupedChart an indication if the chart is grouped
 * @param svgChartArea d3.select of the "g.chart-area" group
 *
 * @returns
 */
export function selectMouseOverElements({
  svgChartArea,
  row = {} as IChartData,
  isGroupedChart = false,
  enableAnimations = true,
}: SelectChartMouseOverProps) {
  let attrSelector = `[data-x="${row.xValue}"]`;

  if (isGroupedChart) {
    attrSelector += `[data-group="${row.primaryGroupValue}"]`;
  }

  let rect = svgChartArea.select(`rect${attrSelector}`);
  let text = svgChartArea.select(`text${attrSelector}`);

  if (enableAnimations) {
    rect = rect
      .transition()
      .delay(vizDelay)
      .duration(vizDurations.withAnimations);
    text = text
      .transition()
      .delay(vizDelay)
      .duration(vizDurations.withAnimations);
  }

  return { rect, text };
}
