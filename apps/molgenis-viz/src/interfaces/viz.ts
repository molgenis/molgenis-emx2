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

export interface vizStandardParams {
  chartId: string;
  table: string;
  title?: string;
  description?: string;
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
