export interface ChartOptions {
  id: String;
  chart: String;
  title?: String;
  description?: String;
  table: String;
  x: String;
  y: String;
  x_type: String;
  y_type: String;
  aggregation?: Boolean;
  sort?: Boolean;
}

export interface chartMargins {
  top: Number;
  right?: Number;
  bottom?: Number;
  left?: Number;
}

export interface BarChartParams {
  chartId: String;
  title?: String;
  description?: String;
  xvar: String;
  yvar: String;
  xMax?: Number;
  yTickValues?: Array<Number>;
  xAxisLabel?: String;
  yAxisLabel?: String;
  yAxisLineBreaker?: String;
  chartData: Array[];
  chartHeight?: Number;
  chartMargins: vizChartMargins;
  barFill?: String;
  barHoverFill?: String;
  barColorPalette?: Object;
  barPaddingInner?: Number;
  barPaddingOuter?: Number;
  barAlign?: Number;
  enableClicks?: Boolean;
  enableAnimation?: Boolean;
}
