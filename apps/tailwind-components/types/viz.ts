export interface Charts {
  id: string;
  title: string;
  description?: string;
  width?: number;
  height?: number;
}

export type LegendPosition = "top" | "bottom";

export interface ChartContext {
  title: string;
  description?: string;
}

export interface ChartLegendOptions {
  legendIsEnabled?: boolean;
  legendIsStacked?: boolean;
  legendPosition?: LegendPosition;
  legendHoverEventsAreEnabled?: boolean;
}

export interface ChartOptions extends ChartLegendOptions {
  hoverEventsAreEnabled?: boolean;
  clickEventsAreEnabled?: boolean;
  animationsAreEnabled?: boolean;
}

export type PieChartData = Record<string, number>;
export type ColorPalette = Record<string, string>;

export interface PieCharts extends Charts, ChartOptions {
  data: PieChartData;
  margins?: number;
  colorPalette?: ColorPalette;
  showValues?: boolean;
  showLabels?: boolean;
  showValuesAsPercentages?: boolean;
  asDonutChart?: boolean;
  strokeColor?: string;
}

export interface ColumnCharts extends Charts, ChartOptions {
  data: any;
  xvar: string;
  yvar: string;
  ymax?: number;
  yTickValues?: number[];
  xAxisLabel?: string;
  yAxisLabel?: string;
  breakXAxisLabelsAt?: string;
  columnColor?: string;
  columnColorOnHover?: string;
  columnBorderColor?: string;
  colorPalette?: ColorPalette;
  marginTop?: number;
  marginRight?: number;
  marginBottom?: number;
  marginLeft?: number;
  showGridLines?: boolean;
}

export interface NumericAxisTickData {
  limit: number;
  ticks: number[];
}

export interface CategoricalAxisTickData {
  count: number;
  domains: string[];
}
