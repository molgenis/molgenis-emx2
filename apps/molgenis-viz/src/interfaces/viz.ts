export interface gqlVariableSubSelectionIF {
  key: string;
  nestedKey?: string;
}

export interface PrepareChartDataIF {
  data: object[];
  chartVariables?: gqlVariableSubSelectionIF[];
}

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

export type vizLegendPosition = "top" | "right" | "bottom" | "left";

export interface vizChartMargins {
  top?: number;
  right?: number;
  bottom?: number;
  left?: number;
}

export interface BarChartParams {
  chartId: string;
  title?: string;
  description?: string;
  table: string;
  chartData?: object[];
  schema?: string;
  xvar: string;
  yvar: string;
  xAxisLabel?: string;
  yAxisLabel?: string;
  chartHeight?: number;
  chartMargins?: vizChartMargins;
  xMax?: number;
  xTickValues?: number[];
  yAxisLineBreaker?: string;
  barFill?: string;
  barHoverFill?: string;
  barColorPalette?: object;
  barPaddingInner?: number;
  barPaddingOuter?: number;
  barAlign?: number;
  enableClicks?: boolean;
  enableAnimation?: boolean;
}

export interface ColumnChartParams {
  chartId: string;
  title?: string;
  description?: string;
  schema?: string;
  table: string;
  chartData?: object[];
  xvar: string;
  yvar: string;
  xAxisLabel?: string;
  yAxisLabel?: string;
  chartHeight?: number;
  chartMargins?: vizChartMargins;
  yMax?: number;
  yTickValues?: number[];
  xAxisLineBreaker?: string;
  columnFill?: string;
  columnHoverFill?: string;
  columnColorPalette?: string;
  columnPaddingInner?: number;
  columnPaddingOuter?: number;
  columnAlign?: number;
  enableClicks?: boolean;
  enableAnimation?: boolean;
}

export interface GroupedColumnChartParams {
  chartId: string;
  title?: string;
  description?: string;
  schema?: string;
  table: string;
  chartData?: object[];
  xvar: string;
  yvar: string;
  group: string;
  xAxisLabel?: string;
  yAxisLabel?: string;
  chartHeight?: number;
  chartMargins?: vizChartMargins;
  yMax?: number;
  yTickValues?: number[];
  xAxisLineBreaker?: string;
  columnFill?: string;
  columnHoverFill?: string;
  columnColorPalette?: string;
  columnPaddingInner?: number;
  columnPaddingOuter?: number;
  columnAlign?: number;
  enableClicks?: boolean;
  enableAnimation?: boolean;
  enableChartLegend?: boolean;
  stackLegend?: boolean;
  enableLegendClicks?: boolean;
}

export interface DataTableParams {
  tableId: string;
  schema?: string;
  table: string;
  columns: string;
  caption?: string;
  enableRowHighlighting?: boolean;
  enableRowClicks?: boolean;
  renderHtml?: boolean;
}

export interface PieChartParams {
  chartId: string;
  title?: string;
  description?: string;
  schema?: string;
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
  enableChartLegend?: boolean;
  stackLegend?: boolean;
  enableLegendClicks?: boolean;
}

export interface ScatterPlotParams {
  chartId: string;
  title?: string;
  description?: string;
  schema?: string;
  table: string;
  xvar: string;
  yvar: string;
  group?: string;
  xMin?: number;
  xMax?: number;
  yMin?: number;
  yMax?: number;
  xTickValues?: number[];
  yTickValues?: number[];
  xAxisLabel?: string;
  yAxisLabel?: string;
  pointRadius?: number;
  pointFill?: string;
  pointFillPalette?: object;
  chartHeight?: number;
  chartMargins?: vizChartMargins;
  enableClicks?: boolean;
  enableTooltip?: boolean;
  tooltipTemplate?: Function;
  enableChartLegend?: boolean;
  stackLegend?: boolean;
  enableLegendClicks?: boolean;
}

export interface GeoMercatorParams {
  chartId: string;
  title?: string;
  description?: string;
  schema?: string;
  table: string;
  rowId: string;
  latitude: string;
  longitude: string;
  group?: string;
  groupColorMappings?: object;
  markerColor?: string;
  markerStroke?: string;
  chartHeight?: number;
  mapCenter?: {
    latitude?: number;
    longitude?: number;
  };
  chartSize?: number;
  chartScale?: number;
  pointRadius?: number;
  showTooltip?: boolean;
  tooltipVariables?: string[];
  tooltipTemplate?: Function;
  enableMarkerClicks?: boolean;
  enableLegendClicks?: boolean;
  enableZoom?: boolean;
  zoomLimits?: number[];
  mapColors?: {
    land?: string;
    border?: string;
    water?: string;
  };
}
