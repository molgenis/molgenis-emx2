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

export type PieChartData = Record<string, number>;
export type ColorPalette = Record<string, string>;

export interface PieCharts extends Charts {
  data: PieChartData;
  margins?: number;
  colorPalette?: ColorPalette;
  showValues?: boolean;
  showLabels?: boolean;
  showValuesAsPercentages?: boolean;
  asDonutChart?: boolean;
  strokeColor?: string;
  hoverEventsAreEnabled?: boolean;
  clickEventsAreEnabled?: boolean;
  legendIsEnabled?: boolean;
  legendIsStacked?: boolean;
  legendPosition?: LegendPosition;
  legendHoverEventsAreEnabled?: boolean;
}
