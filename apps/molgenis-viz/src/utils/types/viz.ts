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

export interface chartMargins {
  top: number;
  right?: number;
  bottom?: number;
  left?: number;
}

export interface BarChartParams {
  chartId: string;
  title?: string;
  description?: string;
  xvar?: string;
  yvar?: string;
  xMax?: number;
  yTickValues?: Array<Number>;
  xAxisLabel?: string;
  yAxisLabel?: string;
  yAxisLineBreaker?: string;
  chartData: Array[];
  chartHeight?: number;
  chartMargins?: vizChartMargins;
  barFill?: string;
  barHoverFill?: string;
  barColorPalette?: object;
  barPaddingInner?: number;
  barPaddingOuter?: number;
  barAlign?: number;
  enableClicks?: boolean;
  enableAnimation?: boolean;
}
