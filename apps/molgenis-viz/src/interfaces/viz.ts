export interface ChartOptions {
  id: string;
  chart: string;
  title?: string;
  description?: string;
  table: string;
  x: string;
  y: string;
  x_type: string;
  y_type: string;
  aggregation?: boolean;
  sort?: boolean;
}

export interface vizChartContext {
  title?: string;
  description?: string;
}

export type vizLegendPosition = "top" | "right" | "bottom" | "left";

export interface vizStandardParams extends vizChartContext {
  chartId: string;
  table: string;
  chartData?: Array[];
  xvar: string;
  yvar: string;
  xAxisLabel?: string;
  yAxisLabel?: string;
  chartHeight?: number;
  chartMargins?: vizChartMargins;
}

export interface vizChartMargins {
  top: number;
  right?: number;
  bottom?: number;
  left?: number;
}

export interface vizInteractivityParams {
  enableClicks?: boolean;
  enableAnimation?: boolean;
}

export interface vizLegendParams {
  enableChartLegend?: boolean;
  stackLegend?: boolean;
  enableLegendClicks?: boolean;
}

export interface BarChartParams
  extends vizStandardParams,
    vizInteractivityParams {
  xMax?: number;
  xTickValues?: Array<Number>;
  yAxisLineBreaker?: string;
  barFill?: string;
  barHoverFill?: string;
  barColorPalette?: object;
  barPaddingInner?: number;
  barPaddingOuter?: number;
  barAlign?: number;
}

export interface ColumnChartParams
  extends vizStandardParams,
    vizInteractivityParams {
  yMax?: number;
  yTickValues?: Array<Number>;
  xAxisLineBreaker?: string;
  columnFill?: string;
  columnHoverFill?: string;
  columnColorPalette?: string;
  columnPaddingInner?: number;
  columnPaddingOuter?: number;
  columnAlign?: number;
}

export interface GroupedColumnChartParams
  extends ColumnChartParams,
    vizStandardParams,
    vizInteractivityParams,
    vizLegendParams {
  group: string;
}

export interface DataTableParams {
  tableId: string;
  table: string;
  columnOrder: string;
  caption?: string;
  enableRowHighlighting?: boolean;
  enableRowClicks?: boolean;
  renderHtml?: boolean;
}

export interface PieChartParams extends vizChartContext, vizLegendParams {
  chartId: string;
  table: string;
  categories: string;
  values: string;
  valuesAreShown?: boolean;
  valuesArePercents?: boolean;
  chartHeight?: number;
  chartMargins?: vizChartMargins;
  chartScale?: number;
  chartColors?: object;
  strokeColor?: string;
  asDonutChart?: boolean;
  centerAlignChart?: boolean;
  legendPosition?: vizLegendPosition;
  enableHoverEvents?: boolean;
  enableClicks?: boolean;
  enableLegendHovering?: boolean;
}

export interface ScatterPlotParams extends vizChartContext {
  chartId: string,
  chartData: Array[],
  xvar: string,
  yvar: string,
  group?: string,
  xMin?: number,
  xMax?: number,
  yMin?: number,
  yMax?: number,
  xTickValues?: Array<number>,
  yTickValues?: Array<number>,
  xAxisLabel?: string,
  yAxisLabel?: string,
  pointRadius?: number
  pointFill?: string,
  pointFillPalette?: object,
  chartHeight?: number,
  chartMargins: vizChartMargins,
  enableClicks?: boolean,
  enabledTooltips?: boolean,
  tooltipTemplate?: function,
  enableChartLegend?: boolean,
  stackLegend?: boolean,
  enableLegendClicks?: boolean
}