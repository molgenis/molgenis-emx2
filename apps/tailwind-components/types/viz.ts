export interface Charts {
  id: string;
  title: string;
  description?: string;
  width?: number;
  height?: number;
}

export type LegendPosition = "top" | "right" | "bottom" | "left";

export interface ChartContext {
  title: string;
  description?: string;
}

export type PieChartData = Record<string, number>;
export type ColorPalette = Record<string, string>;

export interface PieCharts extends Charts {
  data: PieChartData;
  colorPalette?: ColorPalette;
  showValues?: boolean;
  showLabels?: boolean;
  showValuesAsPercentages?: boolean;
  asDonutChart?: boolean;
  strokeColor?: string;
  pieChartIsCentered?: boolean;
  hoverEventsAreEnabled?: boolean;
  clickEventsAreEnabled?: boolean;
  legendIsEnabled?: boolean;
  legendIsStacked?: boolean;
  legendPosition?: LegendPosition;
  legendClickEventsAreEnabled?: boolean;
  legendHoverEventsAreEnabled?: boolean;
  margins?: number;
}
